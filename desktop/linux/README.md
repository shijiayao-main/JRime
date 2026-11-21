# JRime on Linux (IBus shell)

The Linux shell is a thin IBus engine: IBus key events go over the line
protocol to the JRime engine process, and the engine's preedit/commit/candidates
come back through the IBus APIs. Candidate UI is the IBus panel (the
Wayland-safe path; a Compose candidate window is possible on X11 later).

## Files

| file | role |
|---|---|
| `jrime_engine_client.py` | line-protocol client (requests + async `event` lines) |
| `jrime_ibus_engine.py` | IBus engine: key mapping, preedit, lookup table, commit |
| `jrime.xml` | IBus component description (engine id `jrime`) |
| `ibus-engine-jrime` | launcher script |
| `install.sh` | per-user install (files + component XML) |

## Engine package

The engine process is the same Java package the desktop CI builds
(`jrime-engine-linux-x64.zip`: `librime_jni.so` + engine classes + rime assets):

```sh
export JRIME_ENGINE_CLASSPATH=/path/to/engine-classes:/path/to/engine-deps/*
export JRIME_RIME_DYLIB=/path/to/librime_jni.so   # optional, if not on the JVM library path
export JRIME_SHARED_DIR=$HOME/.local/share/jrime/shared
export JRIME_USER_DIR=$HOME/.local/share/jrime/rime
./install.sh
ibus-daemon -drx
```

Then add **JRime 飞鹤快拼** in the input sources (GNOME Settings → Keyboard, or
`ibus-setup`). Shift toggles Chinese/English.

## Status

- Protocol client is unit-tested against a fake engine (`tests/`).
- The IBus shell itself needs a Linux session with `python3-gi` + IBus; it is
  not exercised on macOS CI.
- Known gaps: candidate window is the IBus panel; engine deploy/config reload
  uses the packaged prebuilt data; per-host Wayland/X11 differences are the
  distribution's concern (IBus handles the panel).
