# CORS Middleware

## Goal

Add CORS middleware that checks allowed origins in production and allows all in dev. All HTTP methods allowed for a domain. Minimal custom implementation, no macros, no external dependency.

## Changes

### 1. `src/clj/readx/utils/server.clj` — add `wrap-cors`

Function `(defn wrap-cors [handler allowed-origins])`:

- Reads `Origin` header from request
- If `allowed-origins` is `:all` — reflects back any origin (dev mode)
- If `allowed-origins` is a vector/set — checks membership
- Allowed origin: adds `Access-Control-Allow-Origin`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Credentials` headers
- OPTIONS preflight: returns 200 with CORS headers immediately
- Disallowed origin: passes through without CORS headers (browser blocks)

### 2. `resources/config.edn` — add `:allowed-origins`

```edn
:allowed-origins #profile {:default :all
                           :prod ["https://readx.app"]}
```

### 3. `src/clj/readx/server.clj` — wire middleware

- Add `[server-utils/wrap-cors (:allowed-origins options)]` to the reitit middleware chain (early, before routing)
- Update schema validation to accept `[:or [:= :all] [:vector string?]]`
