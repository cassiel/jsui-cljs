;; tickle: examine objects in the enclosing patcher, annotate them.

(ns example.tickle)

(defn with-rotate
  "Call a function with a rotation in place, then back out of it.
   (I guess calls to this won't nest!)"
  [g amt f]
  (.rotate g amt)
  (f)
  (.identity_matrix g))

(defn do-text
  "Draw text, shifted vertically, within a rotation transformation."
  [g f-size v-offset t]
  (.rel_move_to g 0 v-offset)
  (.set_font_size g f-size)
  (.text_path g t)
  (.stroke g))

(defn paint
  "Repaint on demand."
  [me]
  (let [data (.-data me)
        [my-l my-t my-r my-b] (.-rect (.-box me))
        g (.-mgraphics me)]
    ;; Cycle through the rectangles: pad out each one, and offset
    ;; it by our own position, so that we actually draw around the
    ;; objects in the patcher when we're underneath them.
    (doseq [d data]
      (let [[l t r b] (:rect d)
            padding 5
            bead 10
            l (- (- l padding) my-l)
            t (- (- t padding) my-t)
            r (- (+ r padding) my-l)
            b (- (+ b padding) my-t)]
        ;; Outline rectangle.
        (.set_source_rgba g 0.6 0.3 0 1.0)
        (.rectangle_rounded g l t (- r l) (- b t) bead bead)
        (.stroke g)
        ;; First line of text.
        (.move_to g (+ r padding) (* 0.5 (+ t b)))
        (with-rotate g -0.4
          (fn [] (do-text g 12 0 (:class d))))
        ;; Second line of text. (Some common code here!)
        (.move_to g (+ r padding) (* 0.5 (+ t b)))
        (with-rotate g -0.4
          (fn [] (do-text g 10 12 (:name d))))))))

(defn find-all
  "Find all objects in the patcher. For each, return a map containing the
   object's class, optional bracketted scripting name,  and bounding rectangle."
  [obj]
  (if obj (cons {:name
                 (let [n (.-varname obj)]
                   (if (zero? (count n)) "" (str "<" n ">")))
                 :class (.-maxclass obj)
                 :rect (vec (.-rect obj))}
                (find-all (.-nextobject obj)))
      nil))

(defn bang
  "Force a re-examine and a redraw. The only way to initiate a draw is
   via a `(.redraw)` on `mgraphics`, and we can't pass through any
   parameters, so we plant the data into `this` first."
  [me]
  (set! (.-data me) (find-all (.-firstobject (.-patcher me))))
  (.redraw (.-mgraphics me)))

(defn start-task
  "Kick off a task to periodically scan for objects in the patcher.
   This is rather expensive (hence the slow, 250msec strobe): we
   should only redraw when something has changed."
  [me]
    (let [ticker (js/Task. bang me me)]
    (set! (.-interval ticker) 250)
    (.repeat ticker)
    ;; I don't know why I have to kick it off with `execute`: other examples don't.
    (.execute ticker)))

(defn setup
  "Set up all drawing modes, `paint`, `bang` and the autowatch state.
   Also: start the strobing task to track changes to the patcher."
  [me]
  (let [g (.-mgraphics me)]
    (.init g)
    (set! (.-relative_coords g) 0)      ; Work in pixel coordinates.
    (set! (.-autofill g) 0)
    (set! (.-paint me) (fn [] (paint me)))
    (set! (.-bang me) (fn [] (bang me)))
    (set! (.-autowatch me) 1)
    (start-task me)
    ;;(bang me)
    )
  (let [d (js/Date.)]
    (.post me (str "Loaded example.tickle at " d "\n"))))
