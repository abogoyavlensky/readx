# EPUB Bionic Conversion — Design

## Problem

Users want to convert their EPUB books to bionic reading format. The current app has a converter UI section with file upload (drag-and-drop + click), a "Convert to Bionic EPUB" button, and state management — but no actual backend processing. The conversion button fakes a delay with `js/setTimeout`.

## Proposed Approach

Synchronous server-side EPUB conversion: the frontend sends the file to the backend, the backend reads the EPUB, transforms all text content to bionic format (bold first half of each word), and responds with the converted EPUB file for download.

## Architecture

### Backend

**New dependencies** (in `deps.edn`):

- `nl.siegmann.epublib/epublib-core {:mvn/version "3.1"}` — read/write EPUB files
- `org.jsoup/jsoup {:mvn/version "1.22.1"}` — parse and manipulate XHTML content inside EPUB chapters

**New files:**

1. **`src/clj/readx/utils/bionic.clj`** — Bionic text transformation (JVM-only)
   - `to-bionic-text` — takes a plain text string, returns HTML string with `<b>` tags on the first `ceil(len/2)` characters of each word
   - Uses `java.util.regex.Pattern` with Unicode letter matching (`\p{L}`)
   - Same algorithm as the frontend `to-bionic` in `views.cljs`

2. **`src/clj/readx/utils/epub.clj`** — EPUB processing
   - `read-epub` — reads an `InputStream` into an epublib `Book` object
   - `write-epub` — writes a `Book` to an `OutputStream`/byte array
   - `apply-bionic-to-html` — takes XHTML string, parses with JSoup, walks text nodes, replaces each word with `<b>bold-part</b>rest`, returns modified XHTML string
   - `convert-to-bionic` — orchestrator: reads EPUB → iterates over all content resources (XHTML type) → applies bionic transform to each → returns modified EPUB as byte array

3. **`src/clj/readx/handlers.clj`** (or extend existing handlers) — New API handler
   - `convert-epub-handler` — receives multipart file upload, calls `convert-to-bionic`, returns the result as `application/epub+zip` with `Content-Disposition: attachment`

**New route** (in `routes.cljc` or backend-only route file):

```
POST /api/convert-epub
```

- Request: multipart/form-data with `file` field (the .epub file)
- Response: `application/epub+zip` binary with filename `<original-name>-bionic.epub`

### Frontend

**Modified files:**

1. **`src/cljs/readx/views.cljs`** — `converter-section` changes:
   - Store the actual `File` object (not just filename) in the `selected-file` atom
   - On "Convert" button click: create `FormData`, append the file, POST to `/api/convert-epub`
   - Use `XMLHttpRequest` (or `cljs-ajax`) with `responseType "blob"` to receive binary
   - On success: create a blob URL, trigger download via a temporary `<a>` element, set state to `:done`
   - On error: set state to `:error`, show error message
   - Remove the misleading "processed locally" disclaimer

2. **`src/cljs/readx/events.cljs`** (optional) — Could add re-frame events for the upload, but since the current converter uses local Reagent atoms, we keep it consistent and stay with local state for now.

### Data Flow

```
User drops .epub file
  → clicks "Convert to Bionic EPUB"
  → frontend POSTs file to /api/convert-epub
  → backend reads EPUB with epublib
  → iterates XHTML resources
  → for each resource:
      → parse HTML with JSoup
      → walk text nodes
      → split text into word/non-word tokens
      → wrap first ceil(len/2) chars of each word in <b> tags
      → serialize back to XHTML
  → write modified Book to byte array
  → respond with application/epub+zip
  → frontend receives blob
  → triggers download as "<filename>-bionic.epub"
  → button shows "✓ Conversion complete — download ready"
```

### CSRF Consideration

The app uses `ring-anti-forgery` middleware. The upload request needs to include the CSRF token. Options:
- Embed the CSRF token in the page HTML (already standard with Ring)
- Read it from a meta tag or cookie and include in the request header

### Error Handling

- File too large: return 413 (configure Jetty max upload size if needed)
- Invalid EPUB: return 400 with error message
- Processing error: return 500 with generic error message
- Frontend shows error state on the button with option to retry

## Workplan

- [ ] Add `epublib-core` and `jsoup` dependencies to `deps.edn`
- [ ] Create `src/clj/readx/utils/bionic.clj` with `to-bionic-text` function
- [ ] Create `src/clj/readx/utils/epub.clj` with EPUB read/write and bionic conversion
- [ ] Add `convert-epub-handler` to handlers
- [ ] Add `POST /api/convert-epub` route
- [ ] Update frontend `converter-section` to POST file and trigger download
- [ ] Handle CSRF token in the upload request
- [ ] Test with a real EPUB file
- [ ] Remove "processed locally" disclaimer from the UI

## Notes

- The bionic algorithm is intentionally duplicated (`.cljs` for text preview, `.clj` for EPUB conversion) per design decision — keeps both implementations simple without reader conditionals.
- epublib 3.1 is the latest stable version on Maven Central.
- JSoup 1.22.1 handles malformed XHTML gracefully, which is common in real-world EPUBs.
- The synchronous approach is fine for v1 — most EPUBs are 1–10MB and process quickly. Async/queue-based processing can be added later if needed.
