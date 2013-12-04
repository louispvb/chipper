(defproject chipper "0.1.0-SNAPSHOT"
  :description "A CHIP-8 emulator/debugger/assembler written in Clojure"
  :url "http://github.com/louispvb"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [seesaw "1.4.4"]
                 [com.github.insubstantial/substance "7.1"]]
  :main ^:skip-aot joyclj.core
  :target-path "target/%s"
  ;:profiles {:uberjar {:aot :all}}
  )
