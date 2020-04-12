(ns covid19-mx-time-series.carranco
  (:require [clojure.data.csv :as csv]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [covid19-mx-time-series.sinave :as sinave]))

;; This ns was used to acquire data from the carranco repo
;; for the days before we started collecting data

(def isocodes
  (csv/read-csv
   (slurp "resources/isocodes.csv")))


(def bigtable
  (csv/read-csv
   (slurp "resources/Mexico_COVID19.csv")))


(defn state-isocode
  [r]
  (subs (nth r 1) 3))


(defn state-shortname
  [r]
  (let [short-name (nth r 3)
        long-name (nth r 2)]
    (if (= "" short-name) long-name short-name)))


(def isocode->statename
  (into {} (map (fn [x] {(state-isocode x)
                         (state-shortname x)})
                (rest isocodes))))


(defn isocode->sinave-val
  [sinavefn]
  (into {} (map (fn [x] {(state-isocode x)
                         (sinavefn
                          (isocode->statename
                           (state-isocode x)))})
                (rest isocodes))))


(def isocode->somedec
  (isocode->sinave-val sinave/statename->somedec))


(def isocode->pcode
  (isocode->sinave-val sinave/statename->pcode))


(def isocode->code
  (isocode->sinave-val sinave/statename->code))


(defn only-states
  [r]
  (subvec r 0 (- (count r) 11)))


(defn make-daymaps
  "Creates a list of maps with shape {'fecha' date-val, iso_t val, ...}"
  [parsed-csv]
  (map #(zipmap (only-states (first parsed-csv)) (only-states %))
       (rest parsed-csv)))


(def state-codes
  (group-by #(subs % 0 3)
            (filter #(not= % "Fecha")
                    (keys (first (make-daymaps bigtable))))))

(defn zero-nil [s] (if (or (nil? s) (= s "")) "0" s))


(defn carrancomap->scdmap
  [m]
  (let [state-code (subs (first (first m)) 0 3)
        deaths (get m (str state-code "_D"))
        suspects (get m (str state-code "_S"))
        confirmed (get m state-code)]
    {:deaths (zero-nil deaths)
     :suspects (zero-nil suspects)
     :confirmed (zero-nil confirmed)}))


(defn make-scdmaps
  "Creates a list of maps with shape
  {'fecha' date, isocode {:confirmed val :death val :suspect val}, ...}"
  [x]
  (into {"Fecha" (get x "Fecha")}
        (map (fn [k] {k (carrancomap->scdmap
                         (zipmap (get state-codes k)
                                 (map #(get x %) (get state-codes k))))})
             (keys state-codes))))



(defn format-dmy
  [s]
  (let [parsed (f/parse (f/formatter "yyyy-MM-dd") s)]
    (f/unparse (f/formatter "dd-MM-yyyy") parsed)))


(defn make-sinave-vec
  [[code m]]
  [(get isocode->code code)
   (get isocode->statename code)
   (get isocode->somedec code)
   (get isocode->pcode code)
   (:confirmed m)
   "0"
   (:suspects m)
   (:deaths m)])


(defn make-sinave-daily
  [m]
  {:date (format-dmy (get m "Fecha"))
   :data (sort-by (comp #(Integer/parseInt %) first)
                  (filter #(not= (first %) nil)
                          (map make-sinave-vec (seq m))))})


(defn make-daily
  "Returns vec of daily maps in the shape of sinave using carranco data"
  [parsed-csv]
  (->> parsed-csv
       make-daymaps
       (map make-scdmaps)
       (map make-sinave-daily)))



(defn get-totals
  [row fn]
  (apply + (map (comp #(Integer/parseInt %) fn) (:data row))))

(defn compare-total-deaths
  [local carranco]
  (let [ltotals (map #(get-totals % sinave/state-deaths) local)
        ctotals (subvec (mapv #(get-totals % sinave/state-deaths) carranco)
                        (- (count carranco) (count local)))]
    (= ltotals ctotals)))


(defn compare-regional-day
  [local carranco fn]
  (= (map fn (:data local))
     (map fn (:data carranco))))


(defn compare-regionals
  [local carranco fn]
  (let [carranco' (subvec (vec carranco)
                          (- (count carranco) (count local)))]
    (map #(compare-regional-day (nth local %) (nth carranco' %) fn)
         (range (count local)))))

#_(compare-regionals (sinave/read-daily-states) (make-daily bigtable) sinave/state-suspects)

;; There is a discrepancy in the data for 2020-03-04, suspected cases:
;; We have Jalisco 686, Carranco 687
;; We have Veracruz 403, Carranco 404

#_(compare-regionals (sinave/read-daily-states) (make-daily bigtable) sinave/state-deaths)
#_(compare-regionals (sinave/read-daily-states) (make-daily bigtable) sinave/state-confirmed)

