(ns readx.test-utils
  (:require [clj-reload.core :as reload]
            [integrant.core :as ig]
            [ring.middleware.session.cookie :as ring-session-cookie]
            [ring.middleware.session.store :as ring-session-store]
            [ring.util.codec :as codec]
            [readx.db :as db]
            [readx.server :as server]
            [readx.utils.server :as server-utils]
            [readx.utils.config :as config]))

; TODO: check if needed!
(def ^:const TEST-CSRF-TOKEN "test-csrf-token")
(def ^:const CSRF-TOKEN-HEADER "X-CSRF-Token")
(def ^:const CSRF-TOKEN-SESSION-KEY :ring.middleware.anti-forgery/anti-forgery-token)
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

(defn encrypt-session-to-cookie
  "Encrypt session data to a cookie value using the server's session store."
  [session-data secret-key]
  (-> (ring-session-cookie/cookie-store
        {:key (server-utils/string->16-byte-array secret-key)})
      (ring-session-store/write-session nil session-data)
      (codec/form-encode)))

(defn session-cookies
  "Convert session data to cookies for a request."
  ([session-data]
   (session-cookies session-data TEST-SECRET-KEY))
  ([session-data secret-key]
   {"ring-session" {:value (encrypt-session-to-cookie session-data secret-key)
                    :path "/"
                    :http-only true
                    :secure true}}))

(defn db
  "Get the database connection from the test system."
  []
  (::db/db *test-system*))

(defn server
  "Get the server instance from the test system."
  []
  (::server/server *test-system*))
