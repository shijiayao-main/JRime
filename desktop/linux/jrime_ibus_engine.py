#!/usr/bin/env python3
"""JRime IBus engine (Linux shell).

A thin shell: IBus key events -> line protocol -> engine process; engine replies
-> preedit/commit/lookup table via the IBus APIs. The candidate UI is IBus's
own panel here (the Wayland-safe path); the Compose candidate window is for
platforms where the shell can position a window (X11/Windows).
"""

from __future__ import annotations

import os
import sys
import tempfile

from jrime_engine_client import EngineClient, EngineError, default_engine_command

try:
    import gi

    gi.require_version("IBus", "1.0")
    from gi.repository import IBus, GLib, GObject
except (ImportError, ValueError) as exc:  # pragma: no cover - needs ibus
    print("jrime: python3-gi/IBus missing: %r" % (exc,), file=sys.stderr)
    raise SystemExit(1)

# IBus modifier bits -> librime mask bits (Shift=1, Control=4, Alt=8).
_MASK_SHIFT = 1 << 0
_MASK_CONTROL = 1 << 2
_MASK_ALT = 1 << 3


def _librime_mask(state: int) -> int:
    mask = 0
    if state & IBus.ModifierType.SHIFT_MASK:
        mask |= _MASK_SHIFT
    if state & IBus.ModifierType.CONTROL_MASK:
        mask |= _MASK_CONTROL
    if state & IBus.ModifierType.MOD1_MASK:
        mask |= _MASK_ALT
    return mask


def _repr_for(keyval: int, state: int) -> str | None:
    """Maps an IBus keysym to the engine's key repr."""
    name = IBus.keyval_name(keyval)
    if not name:
        return None
    modifiers = []
    if state & IBus.ModifierType.CONTROL_MASK:
        modifiers.append("Control")
    if state & IBus.ModifierType.MOD1_MASK:
        modifiers.append("Alt")
    if state & IBus.ModifierType.SHIFT_MASK and len(name) == 1 and name.isalpha():
        # Shifted letter: the engine's alphabet is lowercase, shift is a mask.
        modifiers.append("Shift")
        name = name.lower()
    return "+".join(modifiers + [name]) if modifiers else name


class JRimeEngine(IBus.Engine):  # pragma: no cover - needs a live ibus daemon
    __gtype_name__ = "JRimeEngine"

    def __init__(self) -> None:
        super().__init__()
        self._client: EngineClient | None = None
        self._state = {"composing": False, "preedit": ""}

    # -- lifecycle ---------------------------------------------------------
    def do_focus_in(self) -> None:
        if self._client is None:
            shared = os.environ.get("JRIME_SHARED_DIR")
            user = os.environ.get("JRIME_USER_DIR") or os.path.join(
                os.environ.get("XDG_DATA_HOME", os.path.expanduser("~/.local/share")),
                "jrime/rime",
            )
            if not shared:
                print("jrime: JRIME_SHARED_DIR is not set", file=sys.stderr)
                return
            log = os.path.join(tempfile.gettempdir(), "jrime-engine.log")
            self._client = EngineClient(
                default_engine_command(shared, user), stderr_path=log
            )
            self._client.add_event_listener(self._on_engine_event)
            try:
                self._client.start()
            except EngineError as exc:
                print("jrime: engine failed: %r" % (exc,), file=sys.stderr)
                self._client = None

    def do_focus_out(self) -> None:
        self._clear_preedit()

    def do_reset(self) -> None:
        self._clear_preedit()

    # -- keys --------------------------------------------------------------
    def do_process_key_event(self, keyval: int, keycode: int, state: int) -> bool:
        if state & IBus.ModifierType.RELEASE_MASK:
            return False
        if self._client is None:
            return False
        # Bare Shift/CapsLock toggle Chinese/English (parity with the macOS
        # shell). The engine needs the press/release pair: librime's
        # ascii_composer only toggles after a complete shift tap.
        name = IBus.keyval_name(keyval)
        if name in ("Shift_L", "Shift_R", "Caps_Lock"):
            if state & IBus.ModifierType.RELEASE_MASK:
                self._client.key("Release+" + name)
            else:
                reply = self._client.key(name)
                self._client.key("Release+" + name)
                self._apply(reply)
            return True
        repr_ = _repr_for(keyval, state)
        if repr_ is None:
            return False
        try:
            reply = self._client.key(repr_)
        except EngineError:
            return False
        self._apply(reply)
        return bool(reply.get("handled", False))

    # -- candidates --------------------------------------------------------
    def do_candidate_clicked(self, index: int, button: int, state: int) -> None:
        if self._client is None:
            return
        try:
            self._apply(self._client.select(index))
        except EngineError:
            pass
            self._clear_preedit()

    # -- engine -> ibus ----------------------------------------------------
    def _apply(self, reply: dict) -> None:
        preedit = reply.get("preedit") or ""
        composing = bool(reply.get("composing", False))
        self._state = {"composing": composing, "preedit": preedit}
        text = IBus.Text.new_from_string(preedit)
        self.update_preedit_text(text, len(preedit), composing)

        candidates = reply.get("cands") or []
        table = IBus.LookupTable.new(max(1, len(candidates)), 0, True, True)
        for candidate in candidates:
            table.append_candidate(IBus.Text.new_from_string(candidate[0]))
        table.set_cursor_visible(len(candidates) > 0)
        self.update_lookup_table(table, len(candidates) > 0)

        commit = reply.get("commit")
        if commit:
            self.commit_text(IBus.Text.new_from_string(commit))
            self._clear_preedit()

    def _on_engine_event(self, event: dict) -> None:
        commit = event.get("commit")
        if commit:
            self.commit_text(IBus.Text.new_from_string(commit))
        preedit = event.get("preedit") or ""
        if preedit != self._state.get("preedit"):
            composing = bool(event.get("composing", False))
            self._state = {"composing": composing, "preedit": preedit}
            self.update_preedit_text(IBus.Text.new_from_string(preedit), len(preedit), composing)

    def _clear_preedit(self) -> None:
        self._state = {"composing": False, "preedit": ""}
        self.update_preedit_text(IBus.Text.new_from_string(""), 0, False)
        self.update_lookup_table(IBus.LookupTable.new(1, 0, True, True), False)


def main() -> None:  # pragma: no cover - needs a live ibus daemon
    IBus.init()
    GObject.type_register(JRimeEngine)
    bus = IBus.Bus()
    bus.request_name("org.freedesktop.IBus.JRime", 0)
    factory = IBus.Factory.new(bus.get_connection())
    factory.add_engine("jrime", GObject.type_from_name("JRimeEngine"))
    component = IBus.Component.new(
        "org.freedesktop.IBus.JRime",
        "JRime",
        "0.1.0",
        "GPL-3.0-or-later",
        "JRime 飞鹤快拼输入法",
        "https://github.com/jiaoay/jrime",
        "/usr/share/ibus-jrime",
        "ibus-engine-jrime",
    )
    bus.register_component(component)
    GLib.MainLoop().run()


if __name__ == "__main__":
    main()
