(ns scroogecoin.core)
(use '[clojure.java.shell :only [sh]])

;TODO implement data model
(def state (atom {:scrooge-kp {}
                  :blockchain '()
                  :status :valid
                  }))

;TODO insert transaction into the blockchain if it's a valid one
(defn append! [trans] ())

;TODO validate blockchain: true if all connected hashes are equal and (user) signatures are valid
(defn verify [block sign] ())

;TODO return all user-balance key-value pairs
(defn get-balance [block] ())

;TODO init mechanism to determine wether or not the blockchain has been manipulated by Scrooge
(defn supervise [] ())

(defn key-gen []
  (sh "openssl" "genrsa" "-aes256" "-passout" "pass:secret" "-out" "./resources/privkey.pem" "2048")
  (sh "openssl" "rsa" "-pubout" "-in" "./resources/privkey.pem" "-passin" "pass:secret" "-out" "./resources/pubkey.pem"))

;TODO load keypair in repl or atom
(defn init!
  "(re-)generates Scrooge-keypair and saves it to ./resources
  initializes empty Blockchain in atom"
  ([]
    (key-gen)
    (reset! state {:scrooge-kp {}
                   :blockchain '()
                   :status :valid
                   })
    (println "blockchain almost ready!")))

(init!)