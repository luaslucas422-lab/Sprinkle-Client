//go:build windows

package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"

	webview2 "github.com/jchv/go-webview2"
)

func main() {
	if len(os.Args) >= 3 && os.Args[1] == "--selftest" {
		os.Exit(selfTest(os.Args[2]))
	}
	p := paths()
	_ = os.MkdirAll(p.GameDir, 0o755)
	w := webview2.NewWithOptions(webview2.WebViewOptions{
		Debug:     false,
		AutoFocus: true,
		DataPath:  filepath.Join(os.TempDir(), "sprinkle-installer-webview"),
		WindowOptions: webview2.WindowOptions{
			Title:  "Sprinkle Client Installer",
			Width:  560,
			Height: 640,
			IconId: 1,
			Center: true,
		},
	})
	if w == nil {
		showError("The installer needs Microsoft Edge WebView2. Install it from microsoft.com/edge/webview2, or use SprinkleClient-Installer.jar.")
		return
	}
	defer w.Destroy()
	w.SetSize(520, 600, webview2.HintMin)

	var busy sync.Mutex
	call := func(js string) { w.Dispatch(func() { w.Eval(js) }) }

	_ = w.Bind("sprState", func() State { return currentState() })
	_ = w.Bind("sprSave", func(raw string) error {
		var s Settings
		if err := json.Unmarshal([]byte(raw), &s); err != nil {
			return err
		}
		return saveSettings(paths(), s)
	})
	_ = w.Bind("sprInstall", func() {
		go func() {
			if !busy.TryLock() {
				return
			}
			defer busy.Unlock()
			p := paths()
			s := loadSettings(p)
			rep, err := Install(p, s, func(pct float64, msg string) {
				call(fmt.Sprintf("window.onProgress(%f, %s)", pct, jsonString(msg)))
			})
			if err != nil {
				call(fmt.Sprintf("window.onDone(false, %s, null)", jsonString(err.Error())))
				return
			}
			call(fmt.Sprintf("window.onDone(true, %s, %s)", jsonString("Installed! Open the Minecraft Launcher, pick Sprinkle Client and press Play."), jsonString(rep.Skipped)))
		}()
	})
	_ = w.Bind("sprLauncher", func() string {
		if err := OpenMinecraftLauncher(); err != nil {
			return err.Error()
		}
		return ""
	})
	_ = w.Bind("sprFolder", func() { OpenFolder(paths().GameDir) })
	_ = w.Bind("sprClose", func() { w.Terminate() })
	w.SetHtml(buildHTML())
	w.Run()
}
