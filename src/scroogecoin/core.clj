(ns scroogecoin.core
  (:require [buddy.core.hash :as hash]
            [buddy.core.keys :as keys]
            [buddy.core.dsa :as dsa]
            [buddy.core.codecs :refer :all]
            [clojure.java.io :as io])
  (:use [clojure.java.shell :only [sh]]))

(defn -main
  "welcoming main"
  [& args]
  (println "Tach auch. Willkommen auf der Blockchain."))

(def state (atom {}))

(defn hash-key [val]
  (-> (hash/sha256 val)
      (bytes->hex)))

(defn new-block-hash
  ""
  []
  ())

;TODO insert transaction into the blockchain if it's a valid one
(defn append! [trans]
  ())

;TODO validate blockchain: true if all connected hashes are equal and (user) signatures are valid
(defn verify [block sign]
  ())

;TODO return all user-balance key-value pairs
(defn get-balance [block]
  ())

;TODO init mechanism to determine wether or not the blockchain has been manipulated by Scrooge
(defn supervise []
  "init mechanism to determine wether or not the blockchain has been manipulated by Scrooge"
  ())

(defn- key-gen! []
  "uses openssl via terminal to generate a keypair for scrooge and saves it to resources folder"
  (sh "openssl" "ecparam" "-name" "prime256v1" "-out" "./resources/ecparams.pem")
  (sh "openssl" "ecparam" "-in" "./resources/ecparams.pem" "-genkey" "-noout" "-out" "./resources/ec_secret_key.pem")
  (sh "openssl" "ec" "-in" "./resources/ec_secret_key.pem" "-pubout" "-out" "./resources/ec_public_key.pem"))

;(sh "openssl" "genrsa" "-aes256" "-passout" "pass:secret" "-out" "./resources/secret_key.pem" "2048")
;(sh "openssl" "rsa" "-pubout" "-in" "./resources/secret_key.pem" "-passin" "pass:secret" "-out" "./resources/public_key.pem")

;TODO implement data model
(defn init!
  "(re-)generates Scrooge-key-pair and saves it to ./resources
  initializes empty blockchain in atom"
  ([]
    (println "Key-Pair is being created...")
    (key-gen!)
    (reset! state {:blockchain '()
                   :status     :valid
                   :scrooge    {:skey (keys/private-key (io/resource "ec_secret_key.pem"))
                                :pkey (keys/public-key (io/resource "ec_public_key.pem"))}})
    (println "Key-Pair creation finished.\nInitial blockchain created!")))

;(keys/private-key (io/resource "ecprivkey.pem"))
;(keys/public-key (io/resource "ecpubkey.pem"))
;(println @state)


;TODO In der REPL soll ihr Namespace mit use geladen werden???
;(-main)
;Scenario of task 2.3
(init!)                                                     ; 1. Init blockchain
;(supervise)                                                 ; 2. Start supervise mechanism
;(append! ("from Scrooge to Philipp, Coins 3.14" ))          ; 3. Scrooge generates 3.14 coins for Philipp
;(append! ("from Philipp to John, Coins 1"))                 ; 4. Philipp transfers 1 coin to John
;(append! ("from Michael to Philipp, Coins 1"))              ; 5. Michael attempts to transfer 1 coin to Philipp (fails)
;(append! ("from Philipp to Michael, Coins 1"))              ; 6. Philipp transfers 1 coin to Michael
;(verify blockchain)                                         ; 7. Verify blockchain
;(get-balance @state)                                        ; 8. Get Balance
;(init!)                                                     ; 9. Scrooge reinitializes the blockchain. Supervisor-mechanism reports manipulation.