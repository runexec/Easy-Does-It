(ns easy-does-it.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.reader]))

(def ^:dynamic *save-fp*
  (str "today-todo.edn"))

(def app-state
  (atom {:percents [:todo :started :done :removed]
         :tasks []
         :notes []}))

(defn set-percent! [cursor percent-key n]
  (om/update! cursor [percent-key] n))

(defn render-percent! [percent-key]
  (letfn [(calculate-percent [cursor k]
            (let [tasks (:tasks cursor)                  
                  amount (count
                          (filter #(= (:type %) k) tasks))]
              (->> tasks count (/ amount) (* 100) int)))]
    (let [percent-id (str (name percent-key)
                          "-percent")]
    (om/root
     (fn [app owner]
       (om/component
        (dom/span
         (str (calculate-percent app percent-key)
              "%"))))
     app-state
     {:target (. js/document
                 (getElementById percent-id))}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; TASKS


(defn without-task [id cursor]
  (->> (:tasks cursor)
       (remove #(= id (:id %)))))

(defn find-task [id cursor]
  (first
   (filter #(= id (:id %))
           (:tasks cursor))))

(defn new-task [text cursor]
  (om/transact! cursor
                [:tasks]
                (fn [state]
                  (conj state
                        {:id (.getTime (js/Date.))
                         :type :todo
                         :text text}))))

(defcomponent add-task-button-view [cursor _]
  (render
   [_]
   (dom/a {:href "javascript:void(0);"
           :on-click (fn [_]
                       (if-let [text (js/prompt "TODO")]
                         (new-task text cursor)))}
    (dom/span #js {:dangerouslySetInnerHTML #js {:__html "&#10010;"}})
    (dom/span {:class "table icon text"} "ADD"))))


(om/root
 add-task-button-view
 app-state
 {:target (. js/document (getElementById "add-task"))})

(defcomponent edit-task-view [[id cursor] _]
  (render
   [_]
   (let [text (:text (find-task id cursor))]
     (dom/a
      {:href "javascript:void(0);"}
      (dom/div
       {:class "control"
        :on-click
        (fn [_]
          (if-let [text (js/prompt "Edit" text)]
            (om/transact! cursor
                          (fn [cursor]
                            (let [wo (without-task id cursor)
                                  task (find-task id cursor)
                                  task (assoc task :text text)
                                  tasks (vec
                                         (sort-by :id
                                                  (conj wo task)))]
                              (assoc-in cursor [:tasks] tasks))))))}
       "Edit")))))

(defcomponent delete-task-view [[id cursor] _]
  (render
   [_]
   (dom/a {:href "javascript:void(0);"}
          (dom/div
           {:class "control"
            :on-click
            (fn [_]
              (if (js/confirm "WARNING: Delete?")
                (om/transact! cursor
                              (fn [cursor]
                                (let [tasks (without-task id cursor)]
                                  (assoc cursor :tasks tasks))))))}
                   "Delete"))))

(defcomponent task-view [[task cursor] _]
  (render
   [_]
   (let [{:keys [id type]} task
         icon-id (str "icon-" id)
         icon (condp = (:type task)
                :done "&#10007;"
                :started "&#10004;"
                :todo "&#8734;"
                :removed "&#9850;"
                "Icon Not Found")]
     (dom/tr
      (dom/td
       (dom/a
        {:id id
         :href "javascript:void(0);"
         :on-click
         (fn [_]                     
           (om/transact! cursor
                         (fn [state]
                           (let [types (cycle (:percents state))
                                 next-type (second
                                            (drop-while #(not= % type)
                                                        types))
                                 task (assoc (find-task id state)
                                        :type next-type)
                                 tasks (-> id
                                           (without-task state)
                                           (conj task))
                                 tasks (->> tasks
                                            (sort-by :id)
                                             vec)]
                             (assoc state :tasks tasks)))))}
         (dom/div #js {:className "table icon entry"
                       :dangerouslySetInnerHTML #js {:__html icon}})
         (dom/div 
          {:class "table task"} 
          (:text task)
          (dom/br {})))
        (om/build delete-task-view [id cursor])
       (om/build edit-task-view [id cursor]))))))


(defcomponent tasks-view [app owner]
  (render
   [_]
   (let [with-cursor (map #(vector % app) 
                          (:tasks app))]
     (dom/table
      {:class "eigth columns"}
      (dom/tbody 
       (om/build-all task-view with-cursor))))))

(om/root
 tasks-view
 app-state
 {:target (. js/document
             (getElementById "task-table-body"))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; NOTES

(defn without-note [id cursor]
  (->> (:notes cursor)
       (remove #(= id (:id %)))))

(defn find-note [id cursor]
  (first
   (filter #(= id (:id %))
           (:notes cursor))))

(defn new-note [text cursor]
  (om/transact! cursor
                [:notes]
                (fn [state]
                  (conj state
                        {:id (.getTime (js/Date.))
                         :text text}))))

(defcomponent add-note-button-view [cursor _]
  (render
   [_]
   (dom/a {:href "javascript:void(0);"
           :on-click (fn [_]
                       (if-let [text (js/prompt "Note")]
                         (new-note text cursor)))}
    (dom/span #js {:dangerouslySetInnerHTML #js {:__html "&#10010;"}})
    (dom/span {:class "table icon text"} "ADD"))))


(om/root
 add-note-button-view
 app-state
 {:target (. js/document (getElementById "add-note"))})

(defcomponent edit-note-view [[id cursor] _]
  (render
   [_]
   (let [text (:text (find-note id cursor))]
     (dom/a
      {:href "javascript:void(0);"}
      (dom/div
       {:class "control"
        :on-click
        (fn [_]
          (if-let [text (js/prompt "Edit" text)]
            (om/transact! cursor
                          (fn [cursor]
                            (let [wo (without-note id cursor)
                                  note (find-note id cursor)
                                  note (assoc note :text text)
                                  notes (vec
                                         (sort-by :id
                                                  (conj wo note)))]
                              (assoc-in cursor [:notes] notes))))))}
       "Edit")))))

(defcomponent delete-note-view [[id cursor] _]
  (render
   [_]
   (dom/a {:href "javascript:void(0);"}
          (dom/div
           {:class "control"
            :on-click
            (fn [_]
              (if (js/confirm "WARNING: Delete?")
                (om/transact! cursor
                              (fn [cursor]
                                (let [notes (without-note id cursor)]
                                  (assoc cursor :notes notes))))))}
                   "Delete"))))

(defcomponent note-view [[note cursor] _]
  (render
   [_]
   (let [id (:id note)]
     (dom/tr
      (dom/td
       (dom/div 
        {:class "table note"} 
        (:text note)
        (dom/br {})
        (om/build delete-note-view [id cursor])
        (om/build edit-note-view [id cursor])))))))

(defcomponent notes-view [app owner]
  (render
   [_]
   (let [with-cursor (map #(vector % app) 
                          (:notes app))]
     (dom/table
      {:class "eigth columns"}
      (dom/tbody 
       (om/build-all note-view with-cursor))))))

(om/root
 notes-view
 app-state
 {:target (. js/document
             (getElementById "note-table-body"))})



(defn main []
  (render-percent! :done)
  (render-percent! :todo)
  (render-percent! :started)
  (render-percent! :removed)

;;;;;;;;;;;;;;;;;;;;; LOAD MENU


(-> js/document
    (.getElementById "edn-import")
    .-onclick
    (set! (fn [_]
            (if-let [edn (js/prompt "Paste EDN String")]
              (let [edn (cljs.reader/read-string edn)]
                (swap! app-state #(merge % edn)))))))                                             

;;;;;;;;;;;;;;;;;;;; EXPORT MENU

(-> js/document
    (.getElementById "edn-export")
    .-onclick
    (set! (fn [_]
            (let [{:keys [notes tasks]} @app-state
                  m (hash-map :notes notes :tasks tasks)
                  blob (js/Blob. #js [(pr-str m)] 
                                 #js {"type" "text/html"})
                  url (.createObjectURL js/URL blob)]
              (.open js/window url)))))

(-> js/document
    (.getElementById "json-export")
    .-onclick
    (set! (fn [_]
            (let [{:keys [notes tasks]} @app-state
                  json (->> {:notes notes :tasks tasks}
                            clj->js
                           (.stringify js/JSON))
                  blob (js/Blob. #js [json] 
                                 #js {"type" "text/html"})
                  url (.createObjectURL js/URL blob)]
              (.open js/window url))))))
  

