(ns covid19-mx-time-series.sinave
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]))



(defn fetch-daily-states
  []
  (let [map-url "https://covid19.sinave.gob.mx/Mapa.aspx/Grafica22"
        headers {:headers
                 {"Content-Type" "application/json; charset=utf-8"}}
        data (http/post map-url headers)]
    (filter #(not= (second %) "NACIONAL") (json/read-str (:d (json/read-json (:body data)))))))


(defn total-deaths
  [states]
  (apply + (map (comp #(Integer/parseInt %) #(nth % 7))
                states)))


(defn total-confirmed
  [states]
  (apply + (map (comp #(Integer/parseInt %) #(nth % 4))
                states)))


(defn current-deaths
  []
  (total-deaths (fetch-daily-states)))



(defn write-daily-states
  [date data]
  (let [existing (slurp "data/states.edn")
        current (if (= existing "") []  (clojure.tools.reader.edn/read-string existing))]
    (spit "data/states.edn" (pr-str
                             (concat current
                                     [{:date date :data data}])))))

(defn day-mx
  []
  (f/unparse
   (f/formatter "dd-MM-yyyy")
   (t/minus (l/local-now) (t/hours 6))))


(defn fetch-and-write-daily
  []
  (let [s (fetch-daily-states)
        dmx (day-mx)]
    (write-daily-states dmx s)))


(defn read-daily-states
  []
  (clojure.tools.reader.edn/read-string (slurp "data/states.edn")))


(defn state-name
  [day-value]
  (second day-value))


(defn state-confirmed
  [day-value]
  (nth day-value 4))


(defn state-negatives
  [day-value]
  (nth day-value 5))


(defn state-suspects
  [day-value]
  (nth day-value 6))


(defn state-deaths
  [day-value]
  (nth day-value 7))


(defn count-by-state
  [states catfn]
  (into {}
        (map (juxt second (comp #(Integer/parseInt %) catfn))
             states)))


(defn death-counts
  [states]
  (count-by-state states state-deaths))


(defn confirmed-counts
  [states]
  (count-by-state states state-confirmed))


(defn suspect-counts
  [states]
  (count-by-state states state-suspects))


(defn negative-counts
  [states]
  (count-by-state states state-negatives))




(def staterows (:data (first (read-daily-states))))


(def statename->code
  (into {} (map (fn [x] {(state-name x) (first x)})
                staterows)))


(def statename->pcode
  (into {} (map (fn [x] {(state-name x) (nth x 3)})
                staterows)))


(def statename->somedec
  (into {} (map (fn [x] {(state-name x) (nth x 2)})
                staterows)))





(defn make-time-series
  [state-vals value-fn]
  (vec (concat [(state-name (first state-vals))]
               (map value-fn state-vals))))


(defn state-vals
  [daily-states state-pos]
  (doall (map (comp (fn [x] (nth x state-pos)) :data) daily-states)))



