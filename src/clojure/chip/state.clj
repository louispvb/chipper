(ns chip.state)

(defn vector-type
  [type length fill]
  (apply vector-of type (repeat length fill)))

(defn print-mem
  [col-count mem]
  (doseq [xs (partition-all col-count (partition-all 4 mem))]
    (doseq [ys xs]
      (doseq [y ys]
        (printf "%02X" y))
      (print " "))
    (println)))

(defn print-buff
  [buff]
  (doseq [xs (partition-all 64 buff)]
    (doseq [x xs]
      (print (if (= 0 x) \. \#)))
    (println)))

(def font-set
  (vector-of :int
    0xF0, 0x90, 0x90, 0x90, 0xF0, ; 0
    0x20, 0x60, 0x20, 0x20, 0x70, ; 1
    0xF0, 0x10, 0xF0, 0x80, 0xF0, ; 2
    0xF0, 0x10, 0xF0, 0x10, 0xF0, ; 3
    0x90, 0x90, 0xF0, 0x10, 0x10, ; 4
    0xF0, 0x80, 0xF0, 0x10, 0xF0, ; 5
    0xF0, 0x80, 0xF0, 0x90, 0xF0, ; 6
    0xF0, 0x10, 0x20, 0x40, 0x40, ; 7
    0xF0, 0x90, 0xF0, 0x90, 0xF0, ; 8
    0xF0, 0x90, 0xF0, 0x10, 0xF0, ; 9
    0xF0, 0x90, 0xF0, 0x90, 0x90, ; A
    0xE0, 0x90, 0xE0, 0x90, 0xE0, ; B
    0xF0, 0x80, 0x80, 0x80, 0xF0, ; C
    0xE0, 0x90, 0x90, 0x90, 0xE0, ; D
    0xF0, 0x80, 0xF0, 0x80, 0xF0, ; E
    0xF0, 0x80, 0xF0, 0x80, 0x80)); F

(defn merge-vec
  [index update-vec data-vec]
  (loop [i index, acc update-vec]
    (let [data-size (count data-vec)
          rel-pos (- i index)]
      (if (< rel-pos data-size)
        (recur (inc i) (assoc acc i (get data-vec rel-pos)))
        acc))))

; --- MEMORY -------------------
; Each memory cell is 1 byte

; Map:
; 0x000 Start of interpreter mem
; 0x1FF End of interpreter mem
; 0x200 Start of program mem
; 0xFFF End of Chip-8 RAM

(def dmemory (merge-vec 0 (vector-type :int 0x1000 0) font-set))
(def dvx (vector-type :int 0x10 0))
(defn blank-frame-buff [] (vector-type :int (* 64 32) 0))
(def dframe-buff (blank-frame-buff))
(def dkey-vec (vector-type :boolean 0x10 false))

(def buff-width 64)
(def buff-height 32)

(defrecord ChipState
  [^int ip, ; 16bit Instruction Pointer
  ^int pc, ; 16bit Program Counter
  ^int op, ; 16bit Currently executed opcode
  ^int dt, ; 8bit Delay Timer
  ^int st, ; 8bit Sound Timer
  vx, ; 8bit Registers
  key-vec, ; Key state vector (pressed or unpressed)
  stack, ; Stack vector
  ^boolean draw-flag ; Flag used to indicate monitor refresh
  frame-buff, ; Frame buffer
  memory])


(def init-state
  "Initial state of the emulator."
  (ChipState. 0 0 0 0 0
    dvx
    dkey-vec
    []
    false
    dframe-buff
    dmemory))

(defn test-state
  "Non sensical state for running unit tests."
  []
  (ChipState. 0 0 0x0123 0 0
    (vector-of :int
       1 2 3 4
       5 6 7 8
       9 10 11 12
       13 14 15 16)
    dkey-vec
    []
    false
    dframe-buff
    (merge-vec (count font-set) dmemory
      (vector-of :int 1 2 3))))
