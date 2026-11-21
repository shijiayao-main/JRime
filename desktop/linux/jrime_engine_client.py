"""Line-protocol client for the JRime desktop engine.

The engine is any process speaking the protocol described in
``doc/harness-design.md`` §4 and ``desktop/engine/java/.../DesktopServer.kt``:
one JSON request per stdin line, one JSON reply per stdout line. Requests whose
handling happens inside the engine/UI process (candidate clicks in the Compose
window) arrive as asynchronous ``{"event": ...}`` lines; the client surfaces
them to listeners instead of mistaking them for a reply.
"""

from __future__ import annotations

import json
import os
import shlex
import subprocess
import sys
import time
from typing import Any, Callable, Dict, List, Optional


class EngineError(RuntimeError):
    pass


def default_engine_command(shared: str, user: str) -> List[str]:
    """The bundled engine process; overridable for dev/CI via env vars."""
    override = os.environ.get("JRIME_ENGINE_CMD")
    if override:
        return shlex.split(override)
    java = os.environ.get("JRIME_JAVA", "java")
    classpath = os.environ.get("JRIME_ENGINE_CLASSPATH")
    if not classpath:
        raise EngineError(
            "JRIME_ENGINE_CLASSPATH (the engine package classes) is not set; "
            "see desktop/linux/README.md"
        )
    cmd = [java]
    dylib = os.environ.get("JRIME_RIME_DYLIB")
    if dylib:
        cmd.append("-Djava.library.path=" + os.path.dirname(dylib))
    cmd += [
        "-cp",
        classpath,
        "com.jiaoay.jrime.desktop.DesktopServer",
        "--shared",
        shared,
        "--user",
        user,
    ]
    return cmd


class EngineClient:
    """Spawns the engine and speaks the line protocol."""

    def __init__(
        self,
        command: Optional[List[str]] = None,
        ready_timeout: float = 30.0,
        stderr_path: Optional[str] = None,
    ) -> None:
        self._command = command
        self._ready_timeout = ready_timeout
        self._stderr_path = stderr_path
        self._proc: Optional[subprocess.Popen] = None
        self._listeners: List[Callable[[Dict[str, Any]], None]] = []

    # -- lifecycle ---------------------------------------------------------
    def start(self) -> Dict[str, Any]:
        if self._command is None:
            raise EngineError("no engine command configured")
        stderr = subprocess.DEVNULL
        if self._stderr_path:
            stderr = open(self._stderr_path, "a", buffering=1)  # noqa: SIM115
        self._proc = subprocess.Popen(
            self._command,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=stderr,
            text=True,
            bufsize=1,
        )
        reply = self._request("ready")
        # The ready reply also proves the schema mounted.
        if not reply.get("handled", False):
            raise EngineError("engine did not become ready: %r" % (reply,))
        return reply

    def stop(self) -> None:
        proc = self._proc
        if proc is None:
            return
        try:
            if proc.poll() is None:
                self._send_line("exit")
                try:
                    proc.wait(timeout=2)
                except subprocess.TimeoutExpired:
                    proc.kill()
        finally:
            proc = self._proc
            if proc is not None:
                for pipe in (proc.stdin, proc.stdout):
                    try:
                        if pipe is not None:
                            pipe.close()
                    except Exception:
                        pass
            self._proc = None

    @property
    def running(self) -> bool:
        return self._proc is not None and self._proc.poll() is None

    # -- requests ----------------------------------------------------------
    def key(self, repr_: str, mask: int = 0) -> Dict[str, Any]:
        token = repr_ if mask == 0 else "%s@%d" % (repr_, mask)
        # The protocol encodes modifiers in the key repr itself ("Control+a"),
        # so callers should pass a canonical repr; mask is folded by the shell.
        return self._request("k " + token)

    def select(self, index: int) -> Dict[str, Any]:
        return self._request("sel %d" % index)

    def commit(self) -> Dict[str, Any]:
        return self._request("commit")

    def page(self, forward: bool) -> Dict[str, Any]:
        return self._request("pg next" if forward else "pg prev")

    def set_option(self, name: str, value: bool) -> Dict[str, Any]:
        return self._request("opt %s %s" % (name, "true" if value else "false"))

    def rect(self, x: int, y: int, w: int, h: int) -> Dict[str, Any]:
        return self._request("rect %d %d %d %d" % (x, y, w, h))

    # -- events ------------------------------------------------------------
    def add_event_listener(self, listener: Callable[[Dict[str, Any]], None]) -> None:
        self._listeners.append(listener)

    def _notify(self, event: Dict[str, Any]) -> None:
        for listener in list(self._listeners):
            try:
                listener(event)
            except Exception as exc:  # a bad listener must not kill the engine loop
                print("jrime: event listener failed: %r" % (exc,), file=sys.stderr)

    # -- internals ---------------------------------------------------------
    def _send_line(self, line: str) -> None:
        proc = self._proc
        if proc is None or proc.stdin is None:
            raise EngineError("engine is not running")
        proc.stdin.write(line + "\n")
        proc.stdin.flush()

    def _request(self, line: str) -> Dict[str, Any]:
        self._send_line(line)
        return self._read_reply()

    def _read_reply(self) -> Dict[str, Any]:
        proc = self._proc
        if proc is None or proc.stdout is None:
            raise EngineError("engine is not running")
        deadline = time.monotonic() + self._ready_timeout
        while True:
            if time.monotonic() > deadline:
                raise EngineError("engine reply timeout")
            raw = proc.stdout.readline()
            if raw == "":
                raise EngineError("engine process exited")
            text = raw.strip()
            if not text:
                continue
            try:
                data = json.loads(text)
            except json.JSONDecodeError:
                # librime's own stdout noise (should be redirected to stderr,
                # but tolerate strays rather than crashing the IME).
                continue
            if "event" in data:
                self._notify(data)
                continue
            if "error" in data:
                raise EngineError(str(data["error"]))
            return data
