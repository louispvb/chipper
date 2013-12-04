(ns chip.core
  (:use seesaw.core)
  (:import (org.pushingpixels.substance.api SubstanceLookAndFeel)
           (org.pushingpixels.substance.api.skin GraphiteSkin)))

(native!)

(def file-menu
  [(action :name "Open ROM"
     :tip "Loads and executes ROM"
     :key "menu O"
     :handler (fn [e] (alert "Doesn't do anything yet")))
   (action :name "Open Save State"
     :tip "Loads memory snapshot into a save slot")
   :separator
   (action :name "Preferences"
     :tip "Opens general application settings"
     :key "menu shift P")
   :separator
   (action :name "Quit"
     :key "menu Q"
     :handler (fn [e] (.dispose (to-frame e))))])

(def emu-menu
  [(checkbox-menu-item :text "Pause"
     :tip "Pauses or resumes emulation"
     :key "menu P")
   (action :name "Reset"
     :tip "Resets current emulation state of ROM"
     :key "menu R")
   :separator
   (action :name "Increase Speed"
     :tip "Increase clock speed 20%"
     :key "menu I")
   (action :name "Decrease Speed"
     :tip "Decrease clock speed 20%"
     :key "menu D")
   :separator
   (action :name "Save Slot")
   (action :name "Write Slot")
   :separator
   (action :name "Open Debugger"
     :tip "Opens CHIP8 ROM Debugger"
     :key "menu B")])

(def view-menu
  [(checkbox-menu-item :text "Lock Ratio"
     :selected? true
     :tip "Locks canvas ratio to 2:1")
   (action :name "Set Scale"
     :tip "TODO: open submenu with scale factors")
   (action :name "Fullscreen"
     :key "menu F")
   :separator
   (action :name "Take Screenshot"
     :key "menu T")
   :separator
   (checkbox-menu-item :text "Mute Beep"
     :key "menu M")])

(def help-menu
  [(action :name "About"
     :handler (fn [e] (alert "http://github.com")))])

(def chip-menu-bar
  (menubar :items
    [(menu :text "File" :items file-menu)
     (menu :text "Emulation" :items emu-menu)
     (menu :text "View" :items view-menu)
     (menu :text "Help" :items help-menu)]))

(def chip-canvas (canvas :id :canvas :background "#000000" :paint nil))

(def chip-frame (frame :title "Weisbecker"
                  :menubar chip-menu-bar
                  :content (border-panel :center chip-canvas)
                  :resizable? false
                  :size [(* 64 6) :by (* 32 6)]
                  #_(:on-close :exit) ))

(defn center! [frame]
  (.setLocationRelativeTo frame nil)
  frame)

(defn force-top! [frame]
  (.setAlwaysOnTop frame true)
  frame)

(defn -main [& args]
  (invoke-later
    (do
      (when-not (= (System/getProperty "os.name") "Mac OS X")
        (SubstanceLookAndFeel/setSkin (GraphiteSkin.)))
      (-> chip-frame
        ;pack!
        center!
        force-top!
        show!))))