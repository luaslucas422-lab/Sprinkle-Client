package main

import (
	"archive/zip"
	"bytes"
	"embed"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/fs"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"time"
)

const (
	AppVersion = "1.3.0"
	ProfileKey = "sprinkleclient"
	FabricAPI  = "P7dR8mSH" // Modrinth project id of Fabric API
	UserAgent  = "DarkerDawnFR/SprinkleClient-Installer/" + AppVersion
)

// MCVersion is the Minecraft version being installed (picked in the installer).
var MCVersion = "1.21.11"

// Versions the installer offers, newest first.
var Versions = []string{"1.21.11", "1.21.9", "1.21.8", "1.21.1", "1.21"}

//go:embed embed
var embedded embed.FS

//go:embed ui
var uiFiles embed.FS

// ExtraMod is an optional mod the launcher installs from Modrinth.
type ExtraMod struct {
	Slug     string `json:"slug"`
	Name     string `json:"name"`
	Desc     string `json:"desc"`
	Category string `json:"category"`
	Default  bool   `json:"default"`
}

var Catalog = []ExtraMod{
	{"sodium", "Sodium", "Huge FPS boost", "Performance", true},
	{"lithium", "Lithium", "Faster game logic, same gameplay", "Performance", true},
	{"ferrite-core", "FerriteCore", "Uses much less memory", "Performance", true},
	{"immediatelyfast", "ImmediatelyFast", "Faster HUD, text and map drawing", "Performance", true},
	{"entityculling", "Entity Culling", "Skips drawing mobs you can't see", "Performance", true},
	{"iris", "Iris Shaders", "Shaders support (no shader on until you pick one)", "Extras", true},
	{"zoomify", "Zoomify", "Zoom in with C", "Extras", true},
	{"modmenu", "Mod Menu", "See and configure your mods (Donut themed)", "Extras", true},
	{"simple-voice-chat", "Simple Voice Chat", "Proximity voice chat + Donut SMP Soundboard", "Extras", true},
}

// Settings is saved in <gameDir>/launcher.json.
type Settings struct {
	MC   string          `json:"mc"`
	RAM  int             `json:"ram"`
	Mods map[string]bool `json:"mods"`
}

type Paths struct {
	AppData, MCDir, GameDir string
}

func paths() Paths {
	app := os.Getenv("APPDATA")
	if app == "" {
		home, _ := os.UserHomeDir()
		app = filepath.Join(home, ".sprinkle-appdata")
	}
	return Paths{AppData: app, MCDir: filepath.Join(app, ".minecraft"), GameDir: filepath.Join(app, ".sprinkleclient")}
}

func loadSettings(p Paths) Settings {
	s := Settings{MC: "1.21.11", RAM: 4, Mods: map[string]bool{}}
	if b, err := os.ReadFile(filepath.Join(p.GameDir, "installer.json")); err == nil {
		_ = json.Unmarshal(b, &s)
	}
	if s.RAM < 2 || s.RAM > 16 {
		s.RAM = 4
	}
	if s.Mods == nil {
		s.Mods = map[string]bool{}
	}
	for _, m := range Catalog {
		if _, ok := s.Mods[m.Slug]; !ok {
			s.Mods[m.Slug] = m.Default
		}
	}
	return s
}

func saveSettings(p Paths, s Settings) error {
	if err := os.MkdirAll(p.GameDir, 0o755); err != nil {
		return err
	}
	b, _ := json.MarshalIndent(s, "", "  ")
	return os.WriteFile(filepath.Join(p.GameDir, "installer.json"), b, 0o644)
}

// ---------------------------------------------------------------- http helpers

var client = &http.Client{Timeout: 60 * time.Second}

func getJSON(u string, out any) error {
	req, _ := http.NewRequest("GET", u, nil)
	req.Header.Set("User-Agent", UserAgent)
	res, err := client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		return fmt.Errorf("%s returned %d", u, res.StatusCode)
	}
	return json.NewDecoder(res.Body).Decode(out)
}

func download(u, dest string) error {
	req, _ := http.NewRequest("GET", u, nil)
	req.Header.Set("User-Agent", UserAgent)
	res, err := client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		return fmt.Errorf("download %s returned %d", u, res.StatusCode)
	}
	tmp := dest + ".part"
	f, err := os.Create(tmp)
	if err != nil {
		return err
	}
	if _, err = io.Copy(f, res.Body); err != nil {
		f.Close()
		return err
	}
	f.Close()
	return os.Rename(tmp, dest)
}

// ---------------------------------------------------------------- fabric

func fabricProfile() (id string, profile []byte, err error) {
	var loaders []struct {
		Loader struct {
			Version string `json:"version"`
			Stable  bool   `json:"stable"`
		} `json:"loader"`
	}
	if err = getJSON("https://meta.fabricmc.net/v2/versions/loader/"+MCVersion, &loaders); err != nil {
		return
	}
	ver := ""
	for _, l := range loaders {
		if l.Loader.Stable {
			ver = l.Loader.Version
			break
		}
	}
	if ver == "" && len(loaders) > 0 {
		ver = loaders[0].Loader.Version
	}
	if ver == "" {
		return "", nil, errors.New("no Fabric loader found")
	}
	var raw json.RawMessage
	if err = getJSON(fmt.Sprintf("https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json", MCVersion, ver), &raw); err != nil {
		return
	}
	var head struct {
		ID string `json:"id"`
	}
	_ = json.Unmarshal(raw, &head)
	return head.ID, raw, nil
}

func installFabric(p Paths) (string, error) {
	id, prof, err := fabricProfile()
	if err != nil {
		return "", err
	}
	dir := filepath.Join(p.MCDir, "versions", id)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", err
	}
	if err := os.WriteFile(filepath.Join(dir, id+".json"), prof, 0o644); err != nil {
		return "", err
	}
	jar := filepath.Join(dir, id+".jar")
	if _, err := os.Stat(jar); err != nil {
		var buf bytes.Buffer
		zw := zip.NewWriter(&buf)
		zw.Close()
		_ = os.WriteFile(jar, buf.Bytes(), 0o644)
	}
	return id, nil
}

// ---------------------------------------------------------------- modrinth

type mrVersion struct {
	ID         string `json:"id"`
	ProjectID  string `json:"project_id"`
	VersionNum string `json:"version_number"`
	Files      []struct {
		URL      string `json:"url"`
		Filename string `json:"filename"`
		Primary  bool   `json:"primary"`
	} `json:"files"`
	Dependencies []struct {
		ProjectID string `json:"project_id"`
		Type      string `json:"dependency_type"`
	} `json:"dependencies"`
}

func latestVersion(project string) (*mrVersion, error) {
	q := url.Values{}
	q.Set("loaders", `["fabric"]`)
	q.Set("game_versions", `["`+MCVersion+`"]`)
	var vs []mrVersion
	if err := getJSON("https://api.modrinth.com/v2/project/"+url.PathEscape(project)+"/version?"+q.Encode(), &vs); err != nil {
		return nil, err
	}
	if len(vs) == 0 {
		return nil, fmt.Errorf("%s has no version for Minecraft %s", project, MCVersion)
	}
	return &vs[0], nil
}

type manifest map[string]string // slug (or dependency project id) -> file name

func readManifest(p Paths) manifest {
	m := manifest{}
	if b, err := os.ReadFile(filepath.Join(p.GameDir, "sprinkle-installed.json")); err == nil {
		_ = json.Unmarshal(b, &m)
	}
	return m
}

func writeManifest(p Paths, m manifest) {
	b, _ := json.MarshalIndent(m, "", "  ")
	_ = os.WriteFile(filepath.Join(p.GameDir, "sprinkle-installed.json"), b, 0o644)
}

// Report describes what an install did; the self-test prints it.
type Report struct {
	FabricID  string            `json:"fabricId"`
	Installed map[string]string `json:"installed"`
	Skipped   map[string]string `json:"skipped"`
	Profile   string            `json:"profile"`
	ModJar    string            `json:"modJar"`
}

type progressFn func(pct float64, msg string)

// Install puts Fabric, Sprinkle Client and the chosen mods in place and adds the launcher profile.
func Install(p Paths, s Settings, progress progressFn) (*Report, error) {
	r := &Report{Installed: map[string]string{}, Skipped: map[string]string{}}
	if _, err := os.Stat(p.MCDir); err != nil {
		return r, errors.New("Minecraft isn't installed yet. Install the Minecraft Launcher, log in and press Play once, then try again.")
	}
	mods := filepath.Join(p.GameDir, "mods")
	if err := os.MkdirAll(mods, 0o755); err != nil {
		return r, err
	}
	MCVersion = "1.21.11"
	for _, v := range Versions {
		if v == s.MC {
			MCVersion = v
		}
	}
	progress(0.04, "Getting Fabric for Minecraft "+MCVersion+"...")
	id, err := installFabric(p)
	if err != nil {
		return r, fmt.Errorf("could not install Fabric: %w", err)
	}
	r.FabricID = id

	progress(0.12, "Adding sprinkles...")
	jar, err := installSprinkle(mods)
	if err != nil {
		return r, err
	}
	r.ModJar = jar

	// Wanted projects: Fabric API + chosen extras, then their required dependencies.
	want := []string{"fabric-api"}
	for _, m := range Catalog {
		if s.Mods[m.Slug] {
			want = append(want, m.Slug)
		}
	}
	man := readManifest(p)
	keep := map[string]bool{}
	seen := map[string]bool{}
	for i := 0; i < len(want); i++ {
		slug := want[i]
		progress(0.15+0.75*float64(i)/float64(len(want)+1), "Downloading "+prettyName(slug)+"...")
		v, err := latestVersion(slug)
		if err != nil {
			r.Skipped[slug] = err.Error()
			continue
		}
		if seen[v.ProjectID] {
			continue
		}
		seen[v.ProjectID] = true
		if len(v.Files) == 0 {
			r.Skipped[slug] = "no file"
			continue
		}
		f := v.Files[0]
		for _, ff := range v.Files {
			if ff.Primary {
				f = ff
			}
		}
		dest := filepath.Join(mods, f.Filename)
		if _, err := os.Stat(dest); err != nil {
			if err := download(f.URL, dest); err != nil {
				r.Skipped[slug] = err.Error()
				continue
			}
		}
		if old, ok := man[slug]; ok && old != f.Filename {
			_ = os.Remove(filepath.Join(mods, old))
		}
		man[slug] = f.Filename
		keep[slug] = true
		r.Installed[slug] = f.Filename
		for _, d := range v.Dependencies {
			if d.Type == "required" && d.ProjectID != "" && !seen[d.ProjectID] {
				want = append(want, d.ProjectID)
			}
		}
	}
	// Remove mods the player switched off (only ones we installed).
	for key, file := range man {
		if !keep[key] {
			if _, failed := r.Skipped[key]; !failed {
				_ = os.Remove(filepath.Join(mods, file))
				delete(man, key)
			}
		}
	}
	writeManifest(p, man)

	progress(0.93, "Adding Sprinkle Client to the Minecraft Launcher...")
	if err := writeProfile(p, id, s); err != nil {
		return r, fmt.Errorf("could not add the launcher profile: %w", err)
	}
	r.Profile = ProfileKey
	progress(1, "Ready!")
	return r, nil
}

func prettyName(slug string) string {
	for _, m := range Catalog {
		if m.Slug == slug {
			return m.Name
		}
	}
	if slug == "fabric-api" {
		return "Fabric API"
	}
	return "a helper mod"
}

func installSprinkle(mods string) (string, error) {
	var name string
	var data []byte
	_ = fs.WalkDir(embedded, "embed", func(path string, d fs.DirEntry, err error) error {
		if err == nil && !d.IsDir() && strings.HasSuffix(path, "+mc"+MCVersion+".jar") {
			name = filepath.Base(path)
			data, _ = embedded.ReadFile(path)
		}
		return nil
	})
	if data == nil {
		return "", errors.New("this installer has no Sprinkle Client for Minecraft " + MCVersion)
	}
	entries, _ := os.ReadDir(mods)
	for _, e := range entries {
		if strings.HasPrefix(strings.ToLower(e.Name()), "sprinkleclient") && e.Name() != name {
			_ = os.Remove(filepath.Join(mods, e.Name()))
		}
	}
	return name, os.WriteFile(filepath.Join(mods, name), data, 0o644)
}

func iconDataURI() string {
	b, err := uiFiles.ReadFile("ui/profile.png")
	if err != nil {
		return "Furnace"
	}
	return "data:image/png;base64," + base64.StdEncoding.EncodeToString(b)
}

func writeProfile(p Paths, versionID string, s Settings) error {
	file := filepath.Join(p.MCDir, "launcher_profiles.json")
	root := map[string]any{}
	if b, err := os.ReadFile(file); err == nil {
		if err := json.Unmarshal(b, &root); err != nil {
			return fmt.Errorf("launcher_profiles.json is damaged: %w", err)
		}
		_ = os.WriteFile(file+".sprinkle-backup", b, 0o644)
	}
	profiles, _ := root["profiles"].(map[string]any)
	if profiles == nil {
		profiles = map[string]any{}
	}
	now := time.Now().UTC().Format("2006-01-02T15:04:05.000Z")
	prof, _ := profiles[ProfileKey].(map[string]any)
	if prof == nil {
		prof = map[string]any{"created": now}
	}
	prof["name"] = "Sprinkle Client"
	prof["type"] = "custom"
	prof["lastVersionId"] = versionID
	prof["gameDir"] = p.GameDir
	prof["icon"] = iconDataURI()
	prof["lastUsed"] = now
	prof["javaArgs"] = fmt.Sprintf("-Xmx%dG -Xms1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200", s.RAM)
	profiles[ProfileKey] = prof
	root["profiles"] = profiles
	if _, ok := root["version"]; !ok {
		root["version"] = 3
	}
	b, _ := json.MarshalIndent(root, "", "  ")
	return os.WriteFile(file, b, 0o644)
}

// SaveMenuVideo puts the player's own video in the Sprinkle config so it loops next to Rage Quit.
func SaveMenuVideo(p Paths, name string, data []byte) error {
	dir := filepath.Join(p.GameDir, "config", "sprinkleclient", "menu_video")
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	old, _ := os.ReadDir(dir)
	for _, e := range old {
		_ = os.Remove(filepath.Join(dir, e.Name()))
	}
	return os.WriteFile(filepath.Join(dir, filepath.Base(name)), data, 0o644)
}

// RemoveProfile takes Sprinkle Client out of the Minecraft Launcher (keeps worlds and settings).
func RemoveProfile(p Paths) error {
	file := filepath.Join(p.MCDir, "launcher_profiles.json")
	b, err := os.ReadFile(file)
	if err != nil {
		return nil
	}
	root := map[string]any{}
	if err := json.Unmarshal(b, &root); err != nil {
		return err
	}
	if profiles, ok := root["profiles"].(map[string]any); ok {
		delete(profiles, ProfileKey)
	}
	out, _ := json.MarshalIndent(root, "", "  ")
	return os.WriteFile(file, out, 0o644)
}

// OpenMinecraftLauncher starts the official launcher.
func OpenMinecraftLauncher() error {
	if runtime.GOOS != "windows" {
		return errors.New("the Minecraft Launcher can only be opened on Windows")
	}
	cands := []string{
		filepath.Join(os.Getenv("ProgramFiles(x86)"), "Minecraft Launcher", "MinecraftLauncher.exe"),
		filepath.Join(os.Getenv("ProgramFiles"), "Minecraft Launcher", "MinecraftLauncher.exe"),
		`C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe`,
		filepath.Join(os.Getenv("LOCALAPPDATA"), "Programs", "Minecraft Launcher", "MinecraftLauncher.exe"),
	}
	for _, c := range cands {
		if _, err := os.Stat(c); err == nil {
			return exec.Command(c).Start()
		}
	}
	return exec.Command("explorer.exe", `shell:AppsFolder\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft`).Start()
}

// OpenFolder opens a folder in Explorer.
func OpenFolder(dir string) {
	_ = os.MkdirAll(dir, 0o755)
	if runtime.GOOS == "windows" {
		_ = exec.Command("explorer.exe", dir).Start()
	}
}

// catalogByCategory is used by the UI.
func catalogByCategory() map[string][]ExtraMod {
	out := map[string][]ExtraMod{}
	for _, m := range Catalog {
		out[m.Category] = append(out[m.Category], m)
	}
	for k := range out {
		sort.SliceStable(out[k], func(i, j int) bool { return out[k][i].Default && !out[k][j].Default })
	}
	return out
}
