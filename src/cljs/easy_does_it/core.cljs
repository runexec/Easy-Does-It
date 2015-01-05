(ns easy-does-it.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [ajax.core :refer [POST GET]]
            [cognitect.transit :as transit]
            [cljs.reader]))

(defonce app-state 
  (atom {:percent {:done 0
                   :todo 0
                   :started 0
                   :removed 0}
         :tasks []}))

(def task-types [:done :todo :started :removed])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Task Bar Items

(defcomponent task-bar-item [[type percent] _]
  (render
   [_]
   (dom/div
    {:class (str "stat " (name type))}
    (dom/span {:class "percent"} percent "%"))))

(defn new-task []
  (let [id (gensym (-> js/Date .now str))
        eid (str "editor-" id)]
    {:id id
     :type :todo
     :txt "New Task"
     :editor-id eid}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Percentage

(defn calculate-percent! [app]
  (go
    (om/transact! app
                  (fn [state]
                    (let [tasks (:tasks state)
                          each-task (sort
                                     (map :type tasks))
                          hun-percent (count each-task)
                          percent (fn [type]
                                    (let [size (count
                                                (filter #(= % type) each-task))]
                                      (-> size
                                          (/ hun-percent)
                                          (* 100)
                                          (.toPrecision 3))))]
                      (assoc state
                             :percent
                             (apply hash-map
                                    (mapcat #(vector % (percent %))
                                            task-types))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Task Bar

(defcomponent task-bar [app _]
  (will-mount
   [_]
   (calculate-percent! app))
  (render 
   [_]
   (let [{:keys [todo
                 done
                 started
                 removed]} (:percent app)]
     (dom/div 
      (om/build-all task-bar-item
                    [[:done done]
                     [:todo todo]
                     [:started started]
                     [:removed removed]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Task

(defn change-task-type! [task app]
  (let [next-task-type (second
                        (drop-while #(not= (:type @task) %)
                                    (cycle task-types)))]
    (om/update! task [:type] next-task-type))
  (calculate-percent! app))

(defn task-on-click! [_ task app]
  (go
    (change-task-type! task app)))

(defn task-editor-on-change! [evt task]
  (let [txt (-> evt .-target .-value)]
    (go
      (om/update! task [:txt] txt))))

(defn task-edit-on-click! [evt editor-id]
  (let [off-txt "Edit"
        on-txt "Close"
        current-txt (-> evt .-target .-innerHTML)
        [next-txt view] (if (= off-txt current-txt)
                          [on-txt "hover-editor-visible"]
                          [off-txt "hover-editor"])]
    ;; set new text
    (-> evt .-target .-innerHTML (set! next-txt))

    ;; toggle editor display    
    (-> js/document
        (.getElementById editor-id)
        .-className
        (set! view))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Delete Confirm View Toggle

(defn delete-message-show! [& _]
  (go
    (let [id "delete-message"]
      (-> js/document
          (.getElementById id)
          .-style
          .-display
          (set! "block")))))

(defn delete-message-close! [& _]
  (go
    (let [id "delete-message"]
      (-> js/document
          (.getElementById id)
          .-style
          .-display
          (set! "none")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Delete Confirm Event Functions

(defn task-delete-on-click! [_ task app]
  (go
    (om/update! app [:delete-id] (:id @task))
    (delete-message-show!)))

(defn task-delete! [app]
  (go
    (om/transact! app
                  [:tasks]
                  (fn [state]
                    (let [state (vec
                                 (remove #(= (:delete-id app)
                                             (:id %))
                                         state))]
                      state)))
    (calculate-percent! app)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Delete Confirm View

(defcomponent delete-confirm-view [app _]
  (render
   [_]
   (dom/div
    {:id "delete-message"
     :class "row"}
    (dom/div
     {:class "three columns"}
     (dom/h4
      {:id "delete-message-text"}
      "Delete Task"))
    (dom/div
     {:class "four columns"}
     (dom/div 
      {:class "menu-option"} 
      (dom/button
       {:on-click delete-message-close!}
       "Cancel"))
     (dom/div
      {:class "menu-option"} 
      (dom/button {:class "cancel"
                   :on-click (fn [_]
                               (task-delete! app)
                               (delete-message-close!))}
                  "Yes"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Task Item View

(defcomponent task-view [[task app] _]
  (render
   [_]
   (let [{:keys [type txt editor-id]} task
         class (name type)
         on-click #(task-on-click! % task app)
         editor-on-change #(task-editor-on-change! % task)
         delete-on-click #(task-delete-on-click! % task app)
         edit-on-click #(task-edit-on-click! % editor-id)]
     (dom/div {:class (str class " task")}
              (dom/a {:href "javascript:void(0);"
                      :on-click on-click}
                     (dom/pre {:class "txt"} txt))
              (dom/div {:class "task-action-panel"}
                       (dom/a {:class "action"
                               :href "javascript:void(0);"
                               :on-click edit-on-click}
                              "Edit")
                       (dom/a {:class "action"
                               :href "javascript:void(0);"
                               :on-click delete-on-click}
                              "Remove")
                       (dom/textarea {:id editor-id
                                      :class "hover-editor"
                                      :value txt
                                      :on-change editor-on-change}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; All Tasks View

(defcomponent tasks-view [app _]
  (render
   [_]
   (dom/div
    (om/build-all task-view
                  (mapv #(vector % app) (:tasks app))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Add Confirm View Toggle

(defn add-message-show! [& _]
  (go
    (let [id "add-message"]
      (-> js/document
          (.getElementById id)
          .-style
          .-display
          (set! "block")))))

(defn add-message-close! [& _]
  (go
    (let [id "add-message"]
      (-> js/document
          (.getElementById id)
          .-style
          .-display
          (set! "none")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Add Confirm Event Functions

(defn add-to-list! [add-fn app]
  (go
    (om/transact! app
                  [:tasks]
                  (fn [state]
                    (add-fn state)))
    (calculate-percent! app)))

(defn add-to-list-top! [_ app]
  (go
    (let [task (new-task)]
      (println task)
      (add-to-list! (fn [state]
                      (reduce conj [task] state))
                    app)
      (add-message-close!))))

(defn add-to-list-bottom! [_ app]
  (go
    (let [task (new-task)]
      (add-to-list! (fn [state]
                      (conj state task))
                    app)
      (add-message-close!))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Add Confirm View

(defcomponent add-message-view [app _]
  (render
   [_]
   (dom/div
    {:id "add-message"
     :class "row"}
    (dom/div
     {:class "three columns"}
     (dom/h4
      {:id "add-message-text"}
      "Adding Task")) 
    (dom/div 
     {:class "four columns"}
     (dom/div
      {:class "menu-option"} 
      (dom/button
       {:on-click #(add-to-list-top! % app)}
       "Top"))
     (dom/div 
      {:class "menu-option"} 
      (dom/button
       {:on-click #(add-to-list-bottom! % app)}
       "Bottom"))
     (dom/div
      {:class "menu-option"} 
      (dom/button
       {:class "cancel"
        :on-click add-message-close!}
       "Cancel"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Menu View Toggle

(defn menu-message-show! [& _]
  (go
    (let [id "menu-message"]
      (-> js/document
          (.getElementById id)
          .-style
          .-display
          (set! "block")))))

(defn menu-message-close! [& _]
  (go
    (let [id "menu-message"]
      (-> js/document
          (.getElementById id)
          .-style
          .-display
          (set! "none")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; User Action Bar View

(defcomponent button-view [_ _]
  (render
   [_]
   (dom/div
    {:class "row"
     :id "button-row"}
    (dom/a {:href "javascript:void(0);"
            :id "add-button"
            :on-click add-message-show!}
           "+")
    (dom/br {})
    (dom/a {:href "javascript:void(0);"
            :id "menu-button"
            :on-click menu-message-show!}
           "<>"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Task Exportation

(defn export-of-tasks
  ([app]
   (export-of-tasks app true))
  ([app as-string?]
   (let [coll (:tasks @app)]
     (if-not as-string?
       coll
       (pr-str coll)))))

(defn export-tasks!
  [_  app]
  (let [blob (js/Blob. #js [(export-of-tasks app)]
                       #js {"type" "text/html"})
        url (.createObjectURL js/URL blob)]
    (.open js/window url)
    (menu-message-close!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Task Importation

(defn import-tasks!
  [_ app]
  (if-let [tasks (js/prompt "Please paste your exported data")]
    (let [tasks (try
                  (cljs.reader/read-string tasks)
                  (catch js/Object ex [false]))]
      (if-not (every? map? tasks)
        (js/alert "Failed to import. Invalid syntax.")
        (do (om/update! app [:tasks] tasks)
            (calculate-percent! app)
            (menu-message-close!))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Server Save and Load

(defn server-save! [_ app]
  (POST "/save"
        {:params {:tasks (:tasks @app)}
         :format :transit
         :handler (fn [_]
                    (menu-message-close!))
         :error-handler (fn [_]                
                          (js/alert
                           "Couldn't Save! Is the server running?"))}))

(defn server-load! [_ app]
  (GET "/load"
       {:handler (fn [x]
                   (let [{:keys [tasks]} (transit/read (transit/reader :json)
                                                       x)]
                     (om/update! app [:tasks] tasks)
                     (calculate-percent! app)
                     (menu-message-close!)))
        :error-handler (fn [_]
                         (js/alert
                          "Couldn't Save! Is the server running?"))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Main Menu View

(defcomponent menu-view [app _]
  (render
   [_]
   (dom/div
    {:id "menu-message"
     :class "row"}
    (dom/div
     {:class "three columns"}
     (dom/h4 "Easy.does.it"))
    (dom/div
     {:class "four columns"}
     (dom/div {:class "menu-option"} 
              (dom/button
               {:on-click #(export-tasks! % app)}
               "Export"))
     (dom/div {:class "menu-option"} 
              (dom/button
               {:on-click #(import-tasks! % app)}
               "Import"))
     (dom/div {:class "menu-option"}
              (dom/button
               {:on-click #(server-save! % app)}
               "Save"))
     (dom/div {:class "menu-option"
               :on-click #(server-load! % app)}
              (dom/button "Load"))
     (dom/div {:class "menu-option"}
              (dom/button
               {:class "cancel"
                :on-click menu-message-close!} "Cancel"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Application View

(defcomponent main-view [app owner]
  (will-mount
   [_]
   (calculate-percent! app))
  (render
   [_]
   (dom/div
    
    (om/build button-view app)
    
    (om/build add-message-view app)

    (om/build delete-confirm-view app)
    
    (om/build menu-view app)
    
    (dom/div {:class "row"}
             (dom/span {:class "two columns"}
                       (om/build task-bar app))
             (dom/span {:class "one column"}
                       (dom/br {}))
             (dom/span {:id "todo-list" :class "six columns "}
                       (om/build tasks-view app))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Application INIT

(defn main []
  (om/root
   main-view
   app-state
   {:target (. js/document (getElementById "app"))}))
