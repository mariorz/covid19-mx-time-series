(ns covid19-mx-time-series.core
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clj-time.format :as f]
            [covid19-mx-time-series.sinave :as sinave]
            [covid19-mx-time-series.carranco :as carranco]
            [covid19-mx-time-series.dge :as dge]
	    [clj-jgit.porcelain :as jgit]
            [clj-time.format :as f]
            [clj-time.local :as l])
  (:import [java.util Locale]))


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
      (send-kbmsg (str "no update: " last-date " " last-deaths " " last-confirmed)))))
    ;; shutdown bg thread as per
    ;; https://clojureverse.org/t/why-doesnt-my-program-exit/3754/4




;;; series generated from last sinave db snapshot
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; We use the same time frame for all the series
(def series-dates
  (delay (rest (first (csv/read-csv (slurp "data/covid19_confirmed_mx.csv"))))))


(defn ymd->dmy
  [date]
  (f/unparse (f/formatter "dd-MM-yyyy")
             (f/parse (f/formatter "yyyy-MM-dd") date)))


(defn date-row-reducer
  [datefn statefn]
  (fn [accum r]
    (update-in accum [(statefn r)]
               (fn [old arg] (doall (concat old [arg])))
               {(ymd->dmy (datefn r)) 1})))


(defn series-adder
  [accum v]
  (if (last accum)
    (concat accum [(+ (last accum) v)])
    [v]))


(defn make-series
  [selected-rows datefn statefn]
  (->> selected-rows
       (reduce (date-row-reducer datefn statefn) {})
       (map (fn [r]
              {(first r) (apply merge-with + (second r))}))
       (apply merge)
       (map (fn [[k v]]
              (concat [k] (mapv #(get v % 0) @series-dates))))
       (map (fn [r]
              (concat [(first r)]
                      (reduce series-adder [] (rest r)))))
       (sort-by first)))


(defn write-series-csv
  [rows datefn statefn filename]
  (with-open [writer (io/writer filename)]
    (println "writing for:" filename)
    (csv/write-csv
     writer
     (concat [(concat ["Estado"] @series-dates)]
             (make-series rows datefn statefn)))))

(defn pmap-cb [callback f & colls]
        (let [res (doall (apply pmap f colls))]
          (callback)
          res))



#_(defn date-today
  []
  (f/unparse (f/formatter "dd-MM-yyyy")
             (l/local-now)))
;; get repo
;; add data
;; commit with date
;; push origin master
;; keybase msg
#_(defn git-flow
  []
  (let [my-repo (jgit/load-repo "/home/ubuntu/covid19-mx-time-series")]
    (jgit/git-add my-repo "data/")
    (jgit/git-commit my-repo (date-today))
    (jgit/git-push my-repo)))


(defn write-full-state-series-csv
  []
  (let [all-args (apply concat (map (fn [[statefn dirpath]]
                                      [[(dge/deaths @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "deaths_confirmed_by_symptoms_date_mx.csv")]
                                       [(dge/deaths @dge/bigtable) dge/admission-date statefn
                                        (str dirpath "deaths_confirmed_by_admission_date_mx.csv")]
                                       [(dge/deaths @dge/bigtable) dge/death-date statefn
                                        (str dirpath "deaths_confirmed_by_death_date_mx.csv")]
                                       ;; death suspect by symptoms,admission,death
                                       [(dge/deaths-suspects @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "deaths_suspects_by_symptoms_date_mx.csv")]
                                       [(dge/deaths-suspects @dge/bigtable) dge/admission-date statefn
                                        (str dirpath "deaths_suspects_by_admission_date_mx.csv")]
                                       [(dge/deaths-suspects @dge/bigtable) dge/death-date statefn
                                        (str dirpath "deaths_suspects_by_death_date_mx.csv")]
                                       ;; death negative by symptoms,admission,death
                                       [(dge/deaths-negatives @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "deaths_negatives_by_symptoms_date_mx.csv")]
                                       [(dge/deaths-negatives @dge/bigtable) dge/admission-date statefn
                                        (str dirpath "deaths_negatives_by_admission_date_mx.csv")]
                                       [(dge/deaths-negatives @dge/bigtable) dge/death-date statefn
                                        (str dirpath "deaths_negatives_by_death_date_mx.csv")]
                                       ;; confirmed by symptoms date
                                       [(dge/confirmed @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "confirmed_by_symptoms_date_mx.csv")]
                                       ;; suspects by symptoms date
                                       [(dge/suspects @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "suspects_by_symptoms_date_mx.csv")]
                                       ;; negatives by symptoms date
                                       #_[(dge/negatives @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "negatives_by_symptoms_date_mx.csv")]
                                       ;; hospitalized confirmed by symptoms, admission
                                       [(dge/hospitalized-confirmed @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "hospitalized_confirmed_by_symptoms_date_mx.csv")]
                                       [(dge/hospitalized-confirmed @dge/bigtable) dge/admission-date statefn
                                        (str dirpath "hospitalized_confirmed_by_admission_date_mx.csv")]
                                       ;; hospitalized suspect by symptoms, admission
                                       [(dge/hospitalized-suspects @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "hospitalized_suspects_by_symptoms_date_mx.csv")]
                                       [(dge/hospitalized-suspects @dge/bigtable) dge/admission-date statefn
                                        (str dirpath "hospitalized_suspects_by_admission_date_mx.csv")]
                                       ;; hospitalized negatives by symptoms, admission
                                       [(dge/hospitalized-negatives @dge/bigtable) dge/symptoms-date statefn
                                        (str dirpath "hospitalized_suspects_by_symptoms_date_mx.csv")]
                                       [(dge/hospitalized-negatives @dge/bigtable) dge/admission-date statefn
                                        (str dirpath "hospitalized_suspects_by_admission_date_mx.csv")]])
                                    [[dge/state "data/full/by_hospital_state/"]
                                     [dge/residency-state "data/full/by_residency_state/"]]))
        _ (println "all args:" (count all-args))]
    (pmap-cb #(send-kbmsg "finished") #(apply write-series-csv %) all-args)))


(defn date-today
  []
  (f/unparse (f/formatter "dd-MM-yyyy")
             (l/local-now)))
(defn full-flow
  []
  (let [_ (time (write-full-state-series-csv))
        my-repo (jgit/load-repo "/home/mariorz/covid19-mx-time-series")]
    (jgit/git-add my-repo "data/")
    (jgit/git-commit my-repo (str (date-today) "full"))
    (jgit/git-push my-repo)))


(defn basic-flow
  []
  (let [_ (run-write-with-check)
        my-repo (jgit/load-repo "/home/mariorz/covid19-mx-time-series")]
    (jgit/git-add my-repo "data/")
    (jgit/git-commit my-repo (str (date-today)" basic"))
    (jgit/git-push my-repo)))





(defn -main
  [& args]
  (run-write-with-check)
  (println "Generating series for states...")
  (time (write-full-state-series-csv))
  #_(shutdown-agents))
