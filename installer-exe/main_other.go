//go:build !windows

package main

import (
	"fmt"
	"os"
)

// On other systems the .exe only offers the install self-test; use SprinkleClient-Installer.jar instead.
func main() {
	if len(os.Args) >= 3 && os.Args[1] == "--selftest" {
		os.Exit(selfTest(os.Args[2]))
	}
	if len(os.Args) >= 2 && os.Args[1] == "--html" {
		fmt.Print(buildHTML())
		return
	}
	fmt.Println("Sprinkle Client installer " + AppVersion + " runs on Windows. On Mac/Linux use SprinkleClient-Installer.jar.")
}

func showError(msg string) { fmt.Println(msg) }
