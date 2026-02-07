(ns readx.test-utils
  (:require [clj-reload.core :as reload]
            [integrant.core :as ig]
            [readx.db :as db]
            [readx.server :as server]
            [readx.utils.config :as config]))

; TODO: check if needed!
(def ^:const TEST-CSRF-TOKEN "test-csrf-token")
(def ^:const TEST-SECRET-KEY "test-secret-key")

(def ^:dynamic *test-system* nil)

(defn with-system
  "Run the test system before tests."
  ([]
   (with-system nil))
  ([config-path]
   (fn
     [test-fn]
     (let [test-config (config/get-config :test config-path)]
       (ig/load-namespaces test-config)
       (reload/reload)
       (binding [*test-system* (ig/init test-config)]
         (try
           (test-fn)
           (finally
             (ig/halt! *test-system*))))))))

(defn with-truncated-tables
  "Remove all data from all tables except migrations."
  [f]
  (let [db (::db/db *test-system*)]
    (doseq [table (->> {:select [:name]
                        :from [:sqlite_master]
                        :where [:= :type "table"]}
                       (db/exec! db)
                       (map (comp keyword :name)))
            :when (not= :ragtime_migrations table)]
      (db/exec! db {:delete-from table}))
    (f)))

(defn get-server-url
  "Return full url from jetty server object.
  * server - jetty server object
  * env - :host or :container
  :host - localhost
  :container - testcontainers internal host"
  [server]
  (let [base-url "http://localhost:"
        port (.getLocalPort (first (.getConnectors server)))]
    (str base-url port)))

(defn db
  "Get the database connection from the test system."
  []
  (::db/db *test-system*))

(defn server
  "Get the server instance from the test system."
  []
  (::server/server *test-system*))
