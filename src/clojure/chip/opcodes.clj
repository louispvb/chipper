(ns chip.opcodes
  (:use clojure.test
        chip.state))

(with-test
  (defn opbitX
    "Grabs part of the opcode corresponding to X, 0x1X34"
    [state]
    (bit-shift-right (bit-and (:op state) 0x0F00) 8))
  (is (= 1 (opbitX (test-state)))))

(with-test
  (defn opbitY
    "Grabs part of the opcode corresponding to Y, 0x12Y4"
    [state]
    (bit-shift-right (bit-and (:op state) 0x00F0) 4))
  (is (= 2 (opbitY (test-state)))))

(with-test
  (defn opbitN
    "Grabs the last byte of the opcode"
    [state]
    (bit-and (:op state) 0x000F))
  (is (= 3 (opbitN (test-state)))))

(with-test
  (defn opbitNN
    "Grabs the last two bytes of the opcode"
    [state]
    (bit-and (:op state) 0x00FF))
  (is (= 0x23 (opbitNN (test-state)))))

(with-test
  (defn opbitNNN
    "Grabs the last three bytes of the opcode"
    [state]
    (bit-and (:op state) 0x0FFF))
  (is (= 0x123 (opbitNNN (test-state)))))

(with-test
  (defn opregVX
    "Retrieves the register value correspoding to opbitX"
    [state]
    (get-in state [:vx (opbitX state)]))
  (is (= 2 (opregVX (test-state)))))

(with-test
  (defn opregVY
    "Retrieves the register value correspoding to opbitY"
    [state]
    (get-in state [:vx (opbitY state)]))
  (is (= 3 (opregVY (test-state)))))

(with-test
  (defn skip
    "Increments program counter by two (two bytes)"
    [state]
    (assoc state :pc (+ (:pc state) 2)))
  (is (= 2 (:pc (skip (test-state))))))

(defn next-opcode
  "Cycle to next opcode from program memory"
  [{:keys [pc memory] :as state}]
  (skip (assoc state :op (bit-or
                           (bit-shift-left (get memory pc) 8)
                           (get memory (inc pc))))))

(defn setm [m k f v] (assoc m k (f (get m v))))

(defn opID
  [state]
  state)

(defn op00MM
  "Either clear screen or return"
  [state]
  (cond
    (= (opbitNN state) 0xE0) ; Clear screen
    (assoc state :frame-buff (blank-frame-buff))
    (= (opbitNN state) 0xEE) ; Return
    (-> state
      (setm :pc peek :stack )
      (setm :stack pop :stack ))))

(defn op1NNN
  "jmp to 0x0NNN"
  [state]
  (assoc state :pc (opbitNNN state)))

(defn op2NNN
  "call 0x0NNN"
  [state]
  (-> state
    (assoc :stack (conj (get state :pc ) (get state :stack )))
    op1NNN))

(defn op3XNN
  "Skips an instruction if VX == NN"
  [state]
  (if (= (opregVX state) (opbitNN state))
    (skip state)
    state))

(defn op4XNN
  "Skips an instruction if VX != NN"
  [state]
  (if (not= (opregVX state) (opbitNN state))
    (skip state)
    state))

(defn op5XY0
  "Skips an instruction if VX == VY"
  [state]
  (if (= (opregVX state) (opregVY state))
    (skip state)
    state))

(defn op6XNN
  "Sets VX to NN"
  [state]
  (assoc-in state [:vx (opbitX state)] (opbitNN state)))

(defn op7XNN
  "Adds NN to VX"
  [state]
  (update-in state [:vx (opbitX state)] #(+ % (opbitNN state))))

(defn op8XY0
  "Sets VX to VY"
  [state]
  (assoc-in state [:vx (opbitX state)] (get-in state [:vx (opbitY state)])))

(defn op8XYG
  [state f]
  (update-in state [:vx (opbitX state)] #(f % (opregVY state))))

(defn op8XY1
  "Sets VX to VX or VY"
  [state]
  (op8XYG state bit-or))

(defn op8XY2
  "Sets VX to VX and VY"
  [state]
  (op8XYG state bit-and))

(defn op8XY3
  "Sets VX to VX xor VY"
  [state]
  (op8XYG state bit-xor))

(defn setV [state x val]
  (assoc-in state [:vx x] val))
(defn setVX [state val]
  (assoc-in state [:vx (opbitX state)] val))
(defn setVY [state val]
  (assoc-in state [:vx (opbitY state)] val))

(defn op8XY4
  "VX = VX + VY, VF = if carry 1 else 0"
  [state]
  (let [sum (+ (opregVX state) (opregVY state))]
    (-> state
      (setVX (bit-and 0xFF sum))
      (setV 0xF (if (> sum 0xFF) 1 0)))))

(defn op8XY5
  "VX = VX - VY. VF = if borrow 0 else 1"
  [state]
  (let [sub (- (opregVX state) (opregVY state))]
    (-> state
      (setVX (bit-and 0xFF sub))
      (setV 0xF (if (< sub 0) 0 1)))))

(defn op8XY6
  "Shifts VX right by 1. VF set to LSBit of VX before shift"
  [state]
  (-> state
    (setV 0xF (bit-and (opregVX state) 1))
    (setVX (bit-shift-right (opregVX state) 1))))

(defn op8XY7
  "VX = VY - VX. VF set to 0 iff borrow, 1 otherwise"
  [state]
  (let [sub (- (opregVY state) (opregVX state))]
    (-> state
      (setVX (bit-and 0xFF sub))
      (setV 0xF (if (< sub 0) 0 1)))))

(defn op8XYE
  "Shifts VX left by 1. VF set to MSBit of VX before shift"
  [state]
  (-> state
    (setV 0xF (bit-and (bit-shift-right (opregVX state) 7) 1))
    (setVX (bit-shift-left (opregVX state) 1))))

(defn op9XY0
  "Skips next instruction if VX != VY"
  [state]
  (if (not= (opregVX state) (opregVY state))
    (skip state)
    state))

(defn opANNN
  "I = 0x0NNN"
  [state]
  (assoc state :ip (opbitNNN state)))

(defn opBNNN
  "jmp to 0x0NNN + V0"
  [state]
  (assoc state :pc (+ (opbitNNN state) (opregVX state))))

(defn opCXNN
  "Sets VX to a random number and NN"
  [state]
  (setVX state (bit-and (rand-int 0x100) (opbitNN state))))

(defn seq-bits
  "Makes a bit-code seq representation of a byte"
  [byte]
  (loop [b byte, bs [], count 1]
    (if (> count 8)
      (rseq bs)
      (recur (bit-shift-right b 1) (conj bs (bit-and b 1)) (inc count)))))

(defn get-with-default
  "Same as get but supplies a default value if value is not found."
  [v i d]
  (let [g (get v i)] (if g g d)))

(defn vec-replace
  "Replaces part of a vector with another sequence starting at an index.
  The vector will expand to the seq."
  [vs index xs]
  (update-in (pop (reduce
                    (fn [[v flag i] x]
                      [(assoc v i x)
                       (bit-or flag (get-with-default v i 0))
                       (inc i)])
                    [vs 0 index] xs))
    [1] #(not= 0 %)))

(defn vec-slice
  "A workaround to the problem listed
  [here](http://dev.clojure.org/jira/browse/CLJ-1082)."
  [v i j]
  (seq (subvec v i j)))

(defn opDXYN
  "Draw a sprite at (VX, VY) with width of 8px and height of Npx. Each row
  of 8px is read as bit-coded (with MSBit of each byte displayed on the left)
  value starting from memory location I. I value doesn't change after execution
  of this instruction. As described above, VF is set to 1 iff any screen pixels
  are flipped from set to unset when the sprite is drawn (collision check)."
  [{:keys [memory ip frame-buff] :as state}]
  (let [x (opregVX state)
        y (opregVY state)
        height (opbitN state)
        buff-flag
        (reduce
          (fn [[buff flag] i]
            (update-in
              (vec-replace
                buff
                (+ x (* buff-width (+ y i)))
                (seq-bits (get memory (+ ip i))))
              [1] #(or flag %)))
          [frame-buff false]
          (range height))]
    (-> (if (buff-flag 1) (setV state 0xF 1) state)
      (assoc :frame-buff (buff-flag 0)))))

(defn opEXMM
  "9E Skips next instruction if the key stored in VX is pressed.
  A1 Skips next instruction if the key stored in VX isn't pressed."
  [state]
  (cond
    (= (opbitNN state) 0x9E)
    (= (opbitNN state) 0xA1)
    :else (opID state)))

; Lookup and execute FXMM opcode


; VX = DT

; A key press is awaited and then stored in VX


; DT = VX

; ST = VX

; Adds VX to I

; Sets I to the location of the sprite for the character in VX.
; Characters 0-F (in hex) are represented by a 4x5 font.


; Stores the Binary-coded decimal representation of VX, with the most
; significant of 3 digits at the address in I, the middle digit at
; I+1, and the least significant digit at I+2


; Stores V0 to VX in memory starting at address I

; Fills V0 to VX with mem vals starting at address I


#_(def op8-map
     (zipmap (range 0x10)
       [op8XY0, op8XY1, op8XY2, op8XY3, op8XY4, op8XY5, op8XY6, op8XY7,
        opID,   opID,   opID,   opID,   opID,   opID,   op8XYE, opID]))

#_(defn op8XYM
     "Lookup and execute 8XYM opcode"
     [state]
     (let [opcode-fun (get op8-map (opbitN state))]
       (opcode-fun state)))

#_(def op-map
     (zipmap (range 0x10)
       [op00MM, op1NNN, op2NNN, op3XNN, op4XNN, op5XY0, op6XNN, op7XNN,
        op8XYM, op9XY0, opANNN, opBNNN, opCXNN, opDXYN, opEXMM, opFXMM]))

#_(defn execute-cycle
     [state]
     (let [newst (next-opcode state)
           op (:op newst)
           opcode-fun (get op-map (bit-shift-right (bit-and op 0xF000) 12))]
       (recur (opcode-fun state))))