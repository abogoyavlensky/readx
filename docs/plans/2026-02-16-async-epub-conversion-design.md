# Async EPUB Conversion via Dedicated Thread Pool

## Problem

EPUB conversion (`convert-to-bionic`) is CPU-intensive and runs on a Jetty request thread. Concurrent conversions exhaust the Jetty thread pool, blocking the entire server from serving other requests.

## Solution

Offload conversion to a fixed-size thread pool using [claypoole](https://github.com/clj-commons/claypoole). The Jetty thread submits work and waits on the result via `deref`, keeping the request/response model unchanged. The pool size caps concurrent conversions.

## Design

### New dependency

`com.climate/claypoole` in `deps.edn`.

### New Integrant component: `:readx.worker/pool`

File: `src/clj/readx/worker.clj`

- `ig/init-key` creates a `(cp/threadpool size)` from config
- `ig/halt-key!` calls `(cp/shutdown pool)` for graceful drain
- Schema validates `:size` as `pos-int?`

### Configuration

```clojure
; config.edn
:readx.worker/pool {:size #profile {:default 4 :prod 2}}

:readx.server/server {:pool #ig/ref :readx.worker/pool
                      ...}
```

### Handler change

```clojure
; handlers.clj
(let [pool (get-in request [:context :pool])
      result (deref (cp/future pool (epub/convert-to-bionic input-stream)))]
  ...)
```

When all pool threads are busy, `cp/future` queues the task and `deref` blocks until a thread is free. No frontend changes needed.

### Server schema update

Add `:pool` to the server component's Malli schema validation.

## Files changed

1. `deps.edn` - add claypoole dependency
2. `src/clj/readx/worker.clj` - new Integrant component (~20 lines)
3. `resources/config.edn` - add pool config + server ref
4. `src/clj/readx/server.clj` - add `:pool` to schema
5. `src/clj/readx/handlers.clj` - wrap conversion in `cp/future` + `deref`
