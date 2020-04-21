(ns covid19-mx-time-series.core
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [covid19-mx-time-series.sinave :as sinave]
            [covid19-mx-time-series.carranco :as carranco]
            [covid19-mx-time-series.dge :as dge]))


;; currently missing data:
;; 1) suspect cases from 29-02-2020 to 13-03-2020
;; as per https://github.com/carranco-sga/Mexico-COVID-19/issues/1
;; 2) negative cases before 02-04-2020

(defn send-kbmsg
  [msg]
  (shell/sh "keybase" "chat" "send" "covid19mx" msg))



(defn read-daily-states
  []
  (clojure.tools.reader.edn/read-string (slurp "data/states.edn")))


(defn write-timeseries-csv
  [daily-states value-fn filename]
  (with-open [writer (io/writer filename)]
    (let [d (map :data daily-states)
          dates (map :date daily-states)
          state-vecs (map #(sinave/state-vals daily-states %) (range 32))]
      (csv/write-csv writer
                     (concat [(concat ["Estado"] dates)]
                             (map #(sinave/make-time-series % value-fn) state-vecs))))))


(defn write-csvs
  []
  (let [_ (dge/parse-and-write-daily)
        ds (read-daily-states)
        valfns [{:valfn sinave/state-suspects
                 :file "covid19_suspects_mx.csv"}
                {:valfn sinave/state-negatives
                 :file "covid19_negatives_mx.csv"}
                {:valfn sinave/state-confirmed
                 :file "covid19_confirmed_mx.csv"}
                {:valfn sinave/state-deaths
                 :file "covid19_deaths_mx.csv"}]
        dir "data/"]
    (doall (map #(write-timeseries-csv ds (:valfn %) (str dir (:file %))) valfns))))


(defn run-write-with-check
  []
  (let [current-deaths (apply + (vals (dge/death-counts @dge/bigtable)))
        current-confirmed (apply + (vals (dge/confirmed-counts @dge/bigtable)))
        current-date (dge/day-mx)
        r (read-daily-states)
        last-date (:date (last r))
        last-deaths (sinave/total-deaths (:data (last r)))
        last-confirmed (sinave/total-confirmed (:data (last r)))
        _ (println "last:" last-date last-deaths last-confirmed)
        _ (println " now:" current-date current-deaths current-confirmed)]
    (if (and (or (not= current-confirmed last-confirmed)
                 (not= current-deaths last-deaths))
             (not= last-date current-date))
      (do (println "updating...")
          (write-csvs)
          (println "done")
          (send-kbmsg
           (str "updated to: " current-date " " current-deaths " " current-confirmed)))
      (send-kbmsg (str "no update: " last-date " " last-deaths " " last-confirmed)))
    ;; shutdown bg thread as per
    ;; https://clojureverse.org/t/why-doesnt-my-program-exit/3754/4
    (shutdown-agents)))


(defn -main
  [& args]
  (run-write-with-check))
