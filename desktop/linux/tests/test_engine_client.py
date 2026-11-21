"""JVM-free tests for the Linux shell's protocol client."""

import json
import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from jrime_engine_client import EngineClient, EngineError  # noqa: E402

FAKE = [sys.executable, os.path.join(os.path.dirname(__file__), "fake_engine.py")]


class EngineClientTest(unittest.TestCase):
    def setUp(self) -> None:
        self.client = EngineClient(command=FAKE, ready_timeout=5.0)
        self.ready = self.client.start()

    def tearDown(self) -> None:
        self.client.stop()

    def test_ready_handshake(self) -> None:
        self.assertTrue(self.ready["handled"])
        self.assertEqual(self.ready["proto"], 1)
        self.assertEqual(self.ready["engine"], "fake")

    def test_key_round_trip(self) -> None:
        reply = self.client.key("n")
        self.assertEqual(reply["preedit"], "ni")
        self.assertEqual(len(reply["cands"]), 2)

    def test_select_emits_event_and_commit(self) -> None:
        events = []
        self.client.add_event_listener(events.append)
        reply = self.client.select(0)
        self.assertEqual(reply["commit"], "你")
        self.assertEqual(events[0]["event"], "update")
        self.assertEqual(events[0]["commit"], "你")

    def test_rect_is_acknowledged(self) -> None:
        self.assertTrue(self.client.rect(10, 20, 560, 90)["ok"])

    def test_stop_is_idempotent(self) -> None:
        self.client.stop()
        self.client.stop()
        self.assertFalse(self.client.running)

    def test_request_without_engine_raises(self) -> None:
        client = EngineClient(command=None)
        with self.assertRaises(EngineError):
            client.start()


if __name__ == "__main__":
    unittest.main()
