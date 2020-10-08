(ns covid19-mx-time-series.dge
  (:require [clojure.data]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [covid19-mx-time-series.sinave :as sinave]))


(defn day-mx
  []
  (f/unparse
   (f/formatter "dd-MM-yyyy")
   (t/minus (l/local-now) (t/hours 6))))



(defn fetch-csv
  []
  (let [filepath (str "resources/dge." (day-mx) ".csv")
        dge-url "http://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/datos_abiertos_covid19.zip"
        ;;dge-url "http://epidemiologia.salud.gob.mx/gobmx/salud/datos_abiertos/datos_abiertos_covid19.zip"
        ;;dge-url "http://187.191.75.115/gobmx/salud/datos_abiertos/datos_abiertos_covid19.zip"
        stream (->
                (client/get dge-url {:socket-timeout 10000
                                     :connection-timeout 1000
                                     :as :byte-array})
                (:body)
                (io/input-stream)
                (java.util.zip.ZipInputStream.))]
    (.getNextEntry stream)
    (clojure.java.io/copy stream (clojure.java.io/file filepath))
    filepath))

(defn lazy-read-csv
  [csv-file]
  (let [in-file (io/reader csv-file)
        csv-seq (csv/read-csv in-file)
        lazy (fn lazy [wrapped]
               (lazy-seq
                (if-let [s (seq wrapped)]
                  (cons (first s) (lazy (rest s)))
                  (.close in-file))))]
    (lazy csv-seq)))


(def bigtable
  (delay
   (let [_ (println "fetching csv zipfile...")
         filepath (fetch-csv)
         ;;filepath  (str "resources/dge.07-10-2020.csv")
         _ (println "reading csv...")
         ;;r (csv/read-csv
         ;;   (slurp filepath))
         r (lazy-read-csv filepath)]
     (assert (= (count (first r)) 38)
             "Column count in DGE file has changed!")
     r)))


(def state-codes
  ["Aguascalientes"
   "Baja California"
   "Baja California Sur"
   "Campeche"
   "Coahuila"
   "Colima"
   "Chiapas"
   "Chihuahua"
   "Ciudad de México"
   "Durango"
   "Guanajuato"
   "Guerrero"
   "Hidalgo"
   "Jalisco"
   "México"
   "Michoacán"
   "Morelos"
   "Nayarit"
   "Nuevo León"
   "Oaxaca"
   "Puebla"
   "Queretaro"
   "Quintana Roo"
   "San Luis Potosí"
   "Sinaloa"
   "Sonora"
   "Tabasco"
   "Tamaulipas"
   "Tlaxcala"
   "Veracruz"
   "Yucatán"
   "Zacatecas"])

(defn unique-id
  [r]
  (nth r 1))

(defn death-date
  [r]
  (nth r 12))


(defn admission-date
  [r]
  (nth r 10))



(defn symptoms-date
  [r]
  (nth r 11))


(defn sector
  [r]
  (nth r 3))

(defn resultado
  [r]
  (nth r 33))

(defn resultado-lab
  [r]
  (nth r 32))


(defn entidad-res
  [r]
  (nth r 7))


(defn entidad-um
  [r]
  (nth r 4))


(defn- si-no
  [v]
  (case v
    "1" "T"
    "2" "F"
     "NA"))


(defn intubated
  [r]
  (si-no (nth r 13)))



(defn icu
  [r]
  (si-no (nth r 37)))


(defn otro-caso
  [r]
  (si-no (nth r 30)))


(defn toma-muestra
  [r]
  (identity (nth r 31)))


(defn state
  [r]
  (nth state-codes
       (- (Integer/parseInt (entidad-um r)) 1)))


(defn residency-state
  [r]
  (nth state-codes
       (- (Integer/parseInt (entidad-res r)) 1)))


(defn parse-date
  [s]
  (f/parse (f/formatter "yyyy-M-d") s))


(defn deaths
  [csvdata]
  (filter #(and (contains? #{"1" "2" "3"} (resultado %))
                (not= (death-date %) "9999-99-99"))
          (rest csvdata)))




(defn deaths-including-suspects
  [csvdata]
  (filter #(and (or (contains? #{"1" "2" "3"} (resultado %))
                    (= (resultado %) "6"))
                (not= (death-date %) "9999-99-99"))
          (rest csvdata)))


(defn deaths-suspects
  [csvdata]
  (filter #(and (contains? #{"6"} (resultado %))
                (not= (death-date %) "9999-99-99"))
          (rest csvdata)))



(defn deaths-negatives
  [csvdata]
  (filter #(and (= (resultado %) "7")
                (not= (death-date %) "9999-99-99"))
          (rest csvdata)))




(defn confirmed
  [csvdata]
  (filter #(contains? #{"1" "2" "3"} (resultado %)) (rest csvdata)))


(defn suspects
  [csvdata]
  (filter #(contains? #{"6" "5" "4"} (resultado %)) (rest csvdata)))




(defn negatives
  [csvdata]
  (filter #(= (resultado %) "7") (rest csvdata)))


(defn hospitalized-confirmed
  [csvdata]
  (filter #(and (contains? #{"1" "2" "3"} (resultado %))
                (not= (admission-date %) "9999-99-99"))
          (rest csvdata)))


(defn hospitalized-suspects
  [csvdata]
  (filter #(and (= (resultado %) "6")
                (not= (admission-date %) "9999-99-99"))
          (rest csvdata)))


(defn hospitalized-negatives
  [csvdata]
  (filter #(and (= (resultado %) "7")
                (not= (admission-date %) "9999-99-99"))
          (rest csvdata)))




(defn death-counts
  [csvdata]
  (frequencies (map state (deaths csvdata))))


(defn confirmed-counts
  [csvdata]
  (frequencies (map state (confirmed csvdata))))


(defn suspect-counts
  [csvdata]
  (frequencies (map state (suspects csvdata))))


(defn negative-counts
  [csvdata]
  (frequencies (map state (negatives csvdata))))


(defn make-row
  [row-bp deaths confirmed suspects negatives]
  (let [state (second row-bp)
        d (str (get deaths state))
        c (str (get confirmed state))
        s (str (get suspects state))
        n (str (get negatives state))]
    (concat row-bp [c n s d])))


(defn mock-sinave-record
  [csvdata]
  (let [deaths (death-counts csvdata)
        confirmed (confirmed-counts csvdata)
        suspects (suspect-counts csvdata)
        negatives (negative-counts csvdata)
        r (clojure.tools.reader.edn/read-string (slurp "data/states.edn"))
        bp (map #(subvec % 0 4 ) (:data (first r)))
        d (map #(make-row % deaths confirmed suspects negatives) bp)]
    d))



(defn parse-and-write-daily
  []
  (let [dmx (day-mx)
        d (mock-sinave-record @bigtable)]
    (sinave/write-daily-states dmx d)))


#_(def s (sinave/fetch-daily-states))
#_(clojure.data/diff (death-counts bigtable) (sinave/death-counts s))
#_(clojure.data/diff (confirmed-counts bigtable) (sinave/confirmed-counts s))
#_(clojure.data/diff (suspect-counts bigtable) (sinave/suspect-counts s))
#_(clojure.data/diff (negative-counts bigtable) (sinave/negative-counts s)) ;; FAIL


;; errores
;; 0) numero total de casos negativos no corresponde con lo mostrado en el mapa
;; 1) no tenemos fecha de resultado
;; 2) fecha de actualización acutalizan todos en lugar de solo lso que tienen cambios

