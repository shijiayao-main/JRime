# JRime on Windows (TSF shell)

Status: **scaffold + contract** (no Windows toolchain in the dev environment;
the DLL has to be built and smoked on a Windows CI leg).

## Target shape

```
App (any TSF host)  ── ITfTextInputProcessor (in-proc TSF DLL, C++)
        │  keys + composition/candidates
TSF thin DLL  ── line protocol over a named pipe / stdio ──▶  JRime engine JVM
                                                                ├─ librime (desktop bridge)
                                                                └─ :desktop:app CMP candidate window
```

- **Why a DLL + broker, not in-proc JVM**: a TSF text service is an in-proc COM
  DLL; a JVM cannot live in the app process. The DLL is thin: it owns the
  `ITfTextInputProcessor`/`ITfThreadMgrEventSink`/`ITfKeyEventSink` surface and
  forwards key events over the line protocol to one shared engine process.
- **One broker, many hosts**: `ctfmon`/each app loads the DLL; all instances
  connect to a single broker process (`--broker` mode) to avoid a JVM per app.
- **Candidate window**: the broker's `:desktop:app` draws the CMP candidate
  window. The DLL supplies the caret rect from
  `ITfContextView::GetTextExt` via the `rect x y w h` request; the window is a
  layered, `WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW` window (Compose's AWT window
  needs that flag glued on; see the `WS_EX_NOACTIVATE` patch item).
- **Text insertion**: `commitText` is pushed back as the asynchronous
  `{"event":"update","commit":...}` line (or the reply's `commit` for
  shell-initiated requests); the DLL calls `ITfRange::SetText` inside a
  `ITfEditSession`.

## Protocol deltas already in place (D2)

- `rect x y w h` → `{"ok":true}` (candidate-window placement; ignored by the
  headless server).
- `{"event":"update","preedit":...,"composing":...,"commit":...}` pushed by the
  app when a candidate is clicked inside the CMP window.

## Remaining work (next batches)

1. C++ TSF DLL: register text service (GUID, CLSID, profile), key sink →
   repr mapping (`VK_*` → librime reprs), edit session for commit/preedit,
   caret rect query, broker client (named pipe).
2. Broker mode in `:desktop:app` (multi-client, per-host composition state) and
   the `WS_EX_NOACTIVATE` AWT window patch.
3. Installer (per-user registration via `ITfInputProcessorProfiles`), uninstall,
   Authenticode signing notes.
4. Windows CI leg: build DLL (MSVC/ClangCL), start broker, smoke `ready` +
   `k n` + candidate click event; package `jrime-windows-x64.zip`.
