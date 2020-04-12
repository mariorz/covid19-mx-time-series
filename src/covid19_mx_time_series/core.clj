(ns covid19-mx-time-series.core
  (:require [clojure.java.shell :as shell]
            [covid19-mx-time-series.sinave :as sinave]
            [covid19-mx-time-series.carranco :as carranco]))


;; currently missing data:
;; 1) suspect cases from 29-02-2020 to 13-03-2020
;; as per https://github.com/carranco-sga/Mexico-COVID-19/issues/1
;; 2) negative cases before 02-04-2020

(defn send-kbmsg
  [msg]
  (shell/sh "keybase" "chat" "send" "covid19mx" msg))


(defn run-write-with-check
  []
  (let [dcount (sinave/current-deaths)
        dmx (sinave/day-mx)
        r (sinave/read-daily-states)
        last-date (:date (last r))
        ldc (sinave/total-deaths (:data (last r)))
        _ (println "last:" last-date ldc)
        _ (println " now:" dmx dcount)]
    (if (and (not= dcount ldc) (not= last-date dmx))
      (do (println "updating...")
          (sinave/write-all-csvs)
          (println "done")
          (send-kbmsg (str "updated to: " dmx " " dcount)))
      (send-kbmsg (str "no update: " last-date " " ldc)))))


(defn -main
  [& args]
  (run-write-with-check))
