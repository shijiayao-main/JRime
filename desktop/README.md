# Desktop shells

One engine, one protocol, three thin shells. Everything below the shells is
shared: `:desktop:protocol` (line codec/sampler/probe), `:desktop:engine`
(librime bridge + request handling), `:desktop:app` (engine + protocol server +
Compose candidate window in one JVM), `:ui:ime`/`:ui:design`/`:ui:settings`
(Compose Multiplatform UI).

| shell | status | entry |
|---|---|---|
| macOS (IMK, Swift) | built + installed; system candidate window; Shift toggles CN/EN | `macos/JRimeIME/`, `script/install-macos-ime.sh` |
| Linux (IBus, Python) | shell implemented; unit-tested protocol client; needs a Linux session | `desktop/linux/`, `desktop/linux/install.sh` |
| Windows (TSF, C++) | scaffold + protocol contract; no Windows toolchain here | `desktop/windows/README.md` |

## Protocol additions beyond the original v1

- `rect x y w h` — place the candidate window near the caret (shells that can
  draw a window); the headless server ignores it.
- `{"event":"update","preedit","composing","commit"}` — asynchronous push when
  a candidate is clicked inside the Compose window (the shell commits the text
  and refreshes marked text).

`proto` stays 1: additions are backward compatible. The ready handshake
(`proto`, `engine`) keeps shells and engine packages aligned.

## macOS candidate-window note

Public IMK APIs do not expose the caret's screen rect; a self-drawn candidate
window would need accessibility permission. The macOS shell therefore keeps the
system `IMKCandidates` panel, while the Compose candidate window is used where
the caret rect is available (X11, Windows TSF). The settings/status UI is shared
Compose on every platform.
