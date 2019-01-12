(ns scroogecoin.core)
(use '[clojure.java.shell :only [sh]])

(def state (atom {:scrooge-kp {}
                  :blockchain '()
                  :status :valid
                  }))

(defn append! [trans] (println "do something"))

(defn verify [block sign] (println "do something"))

(defn get-balance [block] (println "not yet"))

(defn supervise [] (println "not yet"))

(defn init!
  "generate keypair and save it to ./resources
  init atom with empty list for Blockchain and Scrooge-keypair"
  ([]
    (sh "openssl" "genrsa" "-aes256" "-passout" "pass:secret" "-out" "./resources/privkey.pem" "2048")
    (sh "openssl" "rsa" "-pubout" "-in" "./resources/privkey.pem" "-passin" "pass:secret" "-out" "./resources/pubkey.pem")
    (reset! state {:scrooge-kp {}
                   :blockchain '()
                   :status :valid
                   })
    (println "blockchain almost ready!")))

(init!)