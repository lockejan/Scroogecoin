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

(defn- hash-key [val]
  (-> (hash/sha256 val)
      (bytes->hex)))

(defn- balance-recipient
  [state trx]
  (if (nil? (get-in state [:balance (get trx :recipient)]))
    (assoc-in state [:balance (get trx :recipient)] (get trx :amount))
    (update-in state [:balance (get trx :recipient)] + (get trx :amount))))

(defn- balance-sender
  [state trx]
  (update-in state [:balance (get trx :sender)] - (get trx :amount)))

(defn- update-balance
  ;TODO consider more cases e.g. update-in (when not empty), assoc-in (when empty)
  ""
  [state trx]
  ;(println (get-in state [:balance (first (get trx :recipient))]))
  (cond
    (= (get trx :sender) "Scrooge") (balance-recipient state trx)
    :else (balance-sender (balance-recipient state trx) trx)))

(defn- scrooge-sign
  [state]
  (let [secret (get-in state [:scrooge :skey])
        block (str (first (get-in state [:blockchain])))]
    ;(println "5 " state)
    (assoc-in state [:signature] (dsa/sign block {:key secret :alg :ecdsa+sha256}))))

(defn- prep-trx
  [state hash trx]
  ;(println "4 ")
  (let [bc (get state :blockchain)]
    (scrooge-sign (update-balance (assoc-in state [:blockchain] (conj bc {:hashpointer hash
                                                                          :trx         trx})) trx))))

(defn- hash-trx
  [state trx]
  ;(println "3 ")
  (let [bc (get state :blockchain)]
    ;(println "bc empty?" (empty? bc))
    (cond
      (empty? bc) (prep-trx state nil trx)
      :else (prep-trx state (hash-key (str (first (get-in state [:blockchain])))) trx))))

(defn- trx-check
  "check if sender owes enough coins"
  [state trx]
  (println "2 " (get-in state [:balance] (get trx :sender)))
  (cond
    (= (get trx :sender) :Scrooge) true
    (>= (get-in state [:balance] (get trx :sender)) (get trx :amount)) true
    :else false))

(defn- append
  [state trx]
  ;(println "1 " state)
  (cond
    (empty? trx) state
    (and (> (:amount trx) 0) (trx-check state trx)) (hash-trx state trx)
    :else state))

;TODO insert transaction into the blockchain if it's a valid one
(defn append!
  "append"
  [trans]
  (swap! state append trans))

;TODO validate blockchain: true if all connected hashes are equal and (user) signatures are valid
(defn verify
  "verify"
  [block sign]
  ())

;TODO return all user-balance key-value pairs
(defn get-balance
  "get-balance of all users on the current blockchain state"
  []
  ())

;TODO init mechanism to determine wether or not the blockchain has been manipulated by Scrooge
(defn supervise
  "init mechanism to determine wether or not the blockchain has been manipulated by Scrooge"
  []
  (add-watch state :blockchain
       (fn [key atom old-state new-state]
         ;(if "")
         (prn "-- Atom Changed --")
         ;(prn "key" key)
         ;(prn "atom" atom)
         ;(prn "old-state" old-state)
         ;(prn "new-state" new-state)
         )))

(defn- key-gen! []
  ;"uses openssl via terminal to generate a keypair for scrooge and saves it to resources folder"
  (sh "openssl" "ecparam" "-name" "prime256v1" "-out" "./resources/ecparams.pem")
  (sh "openssl" "ecparam" "-in" "./resources/ecparams.pem" "-genkey" "-noout" "-out" "./resources/ec_secret_key.pem")
  (sh "openssl" "ec" "-in" "./resources/ec_secret_key.pem" "-pubout" "-out" "./resources/ec_public_key.pem"))

; OLD implementation of keygen using aes256.... caused error when attempting to read in secret_key.pem
;(sh "openssl" "genrsa" "-aes256" "-passout" "pass:secret" "-out" "./resources/secret_key.pem" "2048")
;(sh "openssl" "rsa" "-pubout" "-in" "./resources/secret_key.pem" "-passin" "pass:secret" "-out" "./resources/public_key.pem")

;TODO implement data model
(defn init!
  "(re-)generates Scrooge-key-pair and saves it to ./resources
  initializes empty blockchain in atom"
  ([]
    (println "Key-Pair is being created...")
    (key-gen!)
    (reset! state {:blockchain  (list)
                   :signature   {}
                   :status      :valid
                   :scrooge     {:skey (keys/private-key (io/resource "ec_secret_key.pem"))
                                 :pkey (keys/public-key (io/resource "ec_public_key.pem"))}})
    (println "Key-Pair creation finished.\nInitial blockchain created!")))

;(println @state)
(get {:sender :Scrooge :recipient :Philipp :amount 3.14} :sender)

;TODO In der REPL soll ihr Namespace mit use geladen werden???
;(-main)
;Scenarios of task 2.3
(init!)                                                     ; 1. Init blockchain
;(supervise)                                                 ; 2. Start supervise mechanism
(append! {:sender :Scrooge :recipient :Philipp :amount 3.14}) ; 3. Scrooge generates 3.14 coins for Philipp
;(append! {:sender "Philipp" :recipient "John" :amount 1})   ; 4. Philipp transfers 1 coin to John
;(append! {:sender "Michael" :recipient "Philipp" :amount 1}) ; 5. Michael attempts to transfer 1 coin to Philipp (fails)
;(append! {:sender "Philipp" :recipient "Michael" :amount 1}) ; 6. Philipp transfers 1 coin to Michael
;(verify blockchain)                                         ; 7. Verify blockchain
;(get-balance @state)                                        ; 8. Get Balance
;(init!)                                                     ; 9. Scrooge reinitializes the blockchain. Supervisor-mechanism reports manipulation.