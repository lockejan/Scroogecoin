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
  [state trx]
  (cond
    (= (get trx :sender) :Scrooge) (balance-recipient state trx)
    :else (balance-sender (balance-recipient state trx) trx)))

(defn- scrooge-sign
  [state]
  (let [secret (get-in state [:scrooge :skey])
        block (hash-key (str (first (get-in state [:blockchain]))))]
    (assoc-in state [:signature :val] (dsa/sign block {:key secret :alg :ecdsa+sha256}))))

(defn- prep-trx
  [state hash trx]
  (let [bc (get state :blockchain)]
    (scrooge-sign (update-balance (assoc-in state [:blockchain] (conj bc {:hashpointer hash
                                                                          :trx         trx})) trx))))

(defn- hash-trx
  [state trx]
  (let [bc (get state :blockchain)]
    (cond
      (empty? bc) (prep-trx state nil trx)
      :else (prep-trx state (hash-key (str (first (get-in state [:blockchain])))) trx))))

(defn- trx-check
  "check if sender holds enough coins"
  [state trx]
  (cond
    (= (get trx :sender) :Scrooge) true
    (nil? (get-in state [:balance (get trx :sender)])) false
    (>= (get-in state [:balance (get trx :sender)]) (get trx :amount)) true
    :else false))

(defn- append
  [state trx]
  (cond
    (empty? trx) state
    (and (> (:amount trx) 0) (trx-check state trx)) (hash-trx state trx)
    :else state))

(defn append!
  "appends a valid transaktion to the blockchain"
  [trans]
  (swap! state append trans))

;TODO validate blockchain: true if all connected hashes are equal and (user) signatures are valid
(defn verify-bc
  "verify"
  ([blockchain]
    (if (empty? blockchain)
      true
      (verify-bc (last blockchain) (butlast blockchain))))
  ([cur-block blockchain]
    (if (empty? blockchain)
      true
      (and (verify-bc (last blockchain) (butlast blockchain))
           (= (:hashpointer (last blockchain)) (hash-key (str cur-block)))))))

(defn verify
  "expects blockchain and signature. signature contains is a map and contains public key and the signature"
  [blockchain signature]
  (and (verify-bc blockchain)
       (dsa/verify (hash-key (str (first blockchain))) (get-in signature [:val]) {:key (get-in signature [:pkey]) :alg :ecdsa+sha256})))

(defn get-balance!
  "get-balance of all users on the current blockchain state"
  []
  (let [{:keys [balance]} @state]
    (if (empty? balance)
      (println "Alle pleite! Wie langweilig...")
      (println balance))))

;TODO init mechanism to determine wether or not the blockchain has been manipulated by Scrooge
(defn supervise
  "init mechanism to determine wether or not the blockchain has been manipulated by Scrooge"
  []
  (add-watch state :blockchain
       (fn [old-state new-state]
         (if (= old-state new-state)
           (prn "-- Nothing suspicious --")
           (if (= (inc (count old-state)) (count new-state))
             (not= old-state (rest new-state))
             (prn "-- Blockchain Manipulated --"))))))

(defn- key-gen! []
  ;"uses openssl via terminal to generate a keypair for scrooge and saves it to resources folder"
  (sh "openssl" "ecparam" "-name" "prime256v1" "-out" "./resources/ecparams.pem")
  (sh "openssl" "ecparam" "-in" "./resources/ecparams.pem" "-genkey" "-noout" "-out" "./resources/ec_secret_key.pem")
  (sh "openssl" "ec" "-in" "./resources/ec_secret_key.pem" "-pubout" "-out" "./resources/ec_public_key.pem"))

;TODO implement data model
(defn init!
  "(re-)generates Scrooge-key-pair and saves it to ./resources
  initializes empty blockchain in atom"
  ([]
    (println "Key-Pair is being created...")
    (key-gen!)
    (reset! state {:blockchain  (list)
                   :signature   {:pkey (keys/public-key (io/resource "ec_public_key.pem"))}
                   :status      :valid
                   :scrooge     {:skey (keys/private-key (io/resource "ec_secret_key.pem"))}})
    (println "Key-Pair creation finished.\nInitial blockchain created!")))

;TODO In der REPL soll ihr Namespace mit use geladen werden???
;(-main)
;Scenarios of task 2.3
;(init!)                                                               ; 1. Init blockchain
;(supervise)                                                          ; 2. Start supervise mechanism
;(append! {:sender :Scrooge :recipient :Philipp :amount 3.14})         ; 3. Scrooge generates 3.14 coins for Philipp
;(append! {:sender :Philipp :recipient :John :amount 1})               ;4. Philipp transfers 1 coin to John
;(append! {:sender :Michael :recipient :Philipp :amount 1})            ;  5. Michael attempts to transfer 1 coin to Philipp (fails)
;(append! {:sender :Philipp :recipient :Michael :amount 1})            ; 6. Philipp transfers 1 coin to Michael
;(let [{:keys [blockchain signature]} @state]
;  (verify blockchain signature))                                      ; 7. Verify blockchain
;(get-balance!)                                                 ; 8. Get Balance
;(init!)                                                              ; 9. Scrooge reinitializes the blockchain. Supervisor-mechanism reports manipulation.