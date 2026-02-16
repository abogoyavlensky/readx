# Sentry Metrics for ReadX

## Goal

Add meaningful metrics to ReadX using `sentry-clj.metrics` for both operational visibility (performance, errors) and usage analytics (conversion counts, file sizes, traffic patterns).

## Metrics

### HTTP-level (all endpoints)

| Metric | Type | Unit | Attributes | Purpose |
|---|---|---|---|---|
| `http.request` | `increment` | - | `{:method :path :status}` | Request count by endpoint and HTTP status |
| `http.request_duration` | `distribution` | `:millisecond` | `{:method :path}` | Request duration distribution (gives p50/p75/p95/p99) |

### Conversion-specific

| Metric | Type | Unit | Attributes | Purpose |
|---|---|---|---|---|
| `epub.conversion` | `increment` | - | `{:status "success"/"error"}` | Total conversions by outcome |
| `epub.conversion_duration` | `distribution` | `:millisecond` | - | End-to-end conversion time |
| `epub.file_size_input` | `distribution` | `:byte` | - | Upload file size distribution |
| `epub.file_size_output` | `distribution` | `:byte` | - | Output file size distribution |
| `epub.validation_error` | `increment` | - | `{:reason "missing"/"invalid-format"/"too-large"}` | Validation failure count by type |

### Rate limiting

| Metric | Type | Unit | Attributes | Purpose |
|---|---|---|---|---|
| `rate_limit.hit` | `increment` | - | - | Rate limit rejection count |

## Implementation

### 1. HTTP metrics middleware in `server.clj`

Add a `wrap-metrics` Ring middleware that wraps every request:

```clojure
(require '[sentry-clj.metrics :as metrics])

(defn wrap-metrics
  "Ring middleware that records HTTP request count and duration."
  [handler]
  (fn [request]
    (let [start (System/nanoTime)
          response (handler request)
          duration-ms (/ (- (System/nanoTime) start) 1e6)
          method (str/upper-case (name (:request-method request)))
          path (or (:template (:reitit.core/match request))
                   (:uri request))
          status (str (:status response))]
      (metrics/increment "http.request" 1.0 nil {:method method
                                                  :path path
                                                  :status status})
      (metrics/distribution "http.request_duration" duration-ms :millisecond {:method method
                                                                              :path path})
      response)))
```

Place in middleware stack right after `sentry-ring/wrap-sentry-tracing` so it captures the full request lifecycle within the Sentry transaction.

### 2. Conversion metrics in `handlers.clj`

Instrument `convert-epub-handler` directly:

```clojure
(require '[sentry-clj.metrics :as metrics])

(defn convert-epub-handler [request]
  (let [file (get-in request [:multipart-params "file"])
        filename (:filename file)
        file-size ...]
    (cond
      (or (nil? file) (str/blank? filename))
      (do
        (metrics/increment "epub.validation_error" 1.0 nil {:reason "missing"})
        (-> (response/response {:error "No file provided"})
            (response/status 400)))

      (not (str/ends-with? (str/lower-case filename) ".epub"))
      (do
        (metrics/increment "epub.validation_error" 1.0 nil {:reason "invalid-format"})
        (-> (response/response {:error "Only EPUB files are supported"})
            (response/status 400)))

      (> file-size MAX-FILE-SIZE)
      (do
        (metrics/increment "epub.validation_error" 1.0 nil {:reason "too-large"})
        (-> (response/response {:error "File size exceeds 10MB limit"})
            (response/status 413)))

      :else
      (try
        (let [start (System/nanoTime)
              ...
              result @(cp/future pool (epub/convert-to-bionic input-stream))
              duration-ms (/ (- (System/nanoTime) start) 1e6)]
          (metrics/increment "epub.conversion" 1.0 nil {:status "success"})
          (metrics/distribution "epub.conversion_duration" duration-ms :millisecond)
          (metrics/distribution "epub.file_size_input" (double file-size) :byte)
          (metrics/distribution "epub.file_size_output" (double (count result)) :byte)
          ...)
        (catch Exception e
          (metrics/increment "epub.conversion" 1.0 nil {:status "error"})
          ...)))))
```

### 3. Rate limit metric in `limits.clj`

Add a single `increment` call in the rejection branch of `wrap-rate-limit`:

```clojure
(require '[sentry-clj.metrics :as metrics])

; In the rate limit exceeded branch:
(do
  (metrics/increment "rate_limit.hit")
  (-> (response/response {:error "Too many requests. Please try again later."})
      (response/status 429)))
```

## Files to modify

1. **`src/clj/readx/server.clj`** -- Add `wrap-metrics` middleware function and place it in the middleware stack
2. **`src/clj/readx/handlers.clj`** -- Add conversion-specific metrics to `convert-epub-handler`
3. **`src/clj/readx/limits.clj`** -- Add rate limit hit counter

No new files needed. All metrics use `sentry-clj.metrics` directly.

## Middleware stack position

```
... existing middleware ...
[server-utils/wrap-context context]
sentry-ring/wrap-sentry-tracing
wrap-metrics                        ;; <-- NEW: after sentry tracing
ring-parameters/parameters-middleware
... rest of middleware ...
```