package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strings"
)

// State is everything the UI needs when it starts.
type State struct {
	Version   string                `json:"version"`
	MC        string                `json:"mc"`
	Settings  Settings              `json:"settings"`
	Catalog   map[string][]ExtraMod `json:"catalog"`
	MCFound   bool                  `json:"mcFound"`
	Installed bool                  `json:"installed"`
	GameDir   string                `json:"gameDir"`
}

func currentState() State {
	p := paths()
	_, mcErr := os.Stat(p.MCDir)
	_, instErr := os.Stat(p.GameDir + string(os.PathSeparator) + "sprinkle-installed.json")
	return State{
		Version:   AppVersion,
		MC:        MCVersion,
		Settings:  loadSettings(p),
		Catalog:   catalogByCategory(),
		MCFound:   mcErr == nil,
		Installed: instErr == nil,
		GameDir:   p.GameDir,
	}
}

// buildHTML inlines every asset as a data URI so the window needs no web server.
func buildHTML() string {
	html, _ := uiFiles.ReadFile("ui/index.html")
	out := string(html)
	entries, _ := uiFiles.ReadDir("ui")
	for _, e := range entries {
		name := e.Name()
		if !strings.HasSuffix(name, ".png") {
			continue
		}
		b, _ := uiFiles.ReadFile("ui/" + name)
		out = strings.ReplaceAll(out, "{{"+name+"}}", "data:image/png;base64,"+base64.StdEncoding.EncodeToString(b))
	}
	return out
}

func jsonString(v any) string {
	b, _ := json.Marshal(v)
	return string(b)
}

// selfTest installs into a scratch APPDATA without any window; used by the build server.
func selfTest(dir string) int {
	_ = os.Setenv("APPDATA", dir)
	p := paths()
	_ = os.MkdirAll(p.MCDir, 0o755)
	_ = os.WriteFile(p.MCDir+string(os.PathSeparator)+"launcher_profiles.json", []byte(`{"profiles":{"other":{"name":"Keep me","type":"custom"}},"settings":{"keepThis":true},"version":3}`), 0o644)
	s := loadSettings(p)
	_ = saveSettings(p, s)
	rep, err := Install(p, s, func(pct float64, msg string) { fmt.Printf("[%3.0f%%] %s\n", pct*100, msg) })
	fmt.Println("REPORT " + jsonString(rep))
	if err != nil {
		fmt.Println("FAIL install: " + err.Error())
		return 1
	}
	b, _ := os.ReadFile(p.MCDir + string(os.PathSeparator) + "launcher_profiles.json")
	var root map[string]any
	_ = json.Unmarshal(b, &root)
	profiles, _ := root["profiles"].(map[string]any)
	ok := profiles["other"] != nil && profiles[ProfileKey] != nil && root["settings"] != nil
	fmt.Printf("CHECK profile kept other entries: %v\n", ok)
	if !ok {
		return 1
	}
	if rep.Installed["fabric-api"] == "" || rep.ModJar == "" {
		fmt.Println("FAIL fabric api or Sprinkle Client missing")
		return 1
	}
	for _, m := range Catalog {
		if rep.Installed[m.Slug] == "" {
			fmt.Printf("WARN %s not installed: %s\n", m.Slug, rep.Skipped[m.Slug])
		}
	}
	s.Mods["zoomify"] = false
	if _, err := Install(p, s, func(float64, string) {}); err != nil {
		fmt.Println("FAIL reinstall: " + err.Error())
		return 1
	}
	if f := rep.Installed["zoomify"]; f != "" {
		if _, err := os.Stat(p.GameDir + "/mods/" + f); err == nil {
			fmt.Println("FAIL zoomify was not removed")
			return 1
		}
	}
	fmt.Println("PASS installer self-test")
	return 0
}
