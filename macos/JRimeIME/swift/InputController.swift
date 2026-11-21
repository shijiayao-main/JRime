import AppKit
import InputMethodKit

/// IMK input controller: engine key events in, marked text (inline preedit) +
/// system candidate window (IMKCandidates) out. Keys the engine reports as
/// unprocessed fall through to macOS, which inserts the raw character — the
/// same app-layer semantics the harness established (handled:false).
final class InputController: IMKInputController {
    private var engine: EngineClient?
    private var candidatesWindow: IMKCandidates?
    private var lastReply: ServerReply?
    private var lastPreedit = ""
    private weak var currentClient: IMKTextInput?
    private let imkServer: IMKServer

    override init(server: IMKServer!, delegate: Any!, client: Any!) {
        self.imkServer = server
        super.init(server: server, delegate: delegate, client: client)
    }

    // MARK: IMK lifecycle

    override func activateServer(_ sender: Any!) {
        super.activateServer(sender)
        ensureEngine()
        if candidatesWindow == nil {
            candidatesWindow = IMKCandidates(
                server: imkServer, panelType: kIMKSingleColumnScrollingCandidatePanel)
        }
    }

    override func deactivateServer(_ sender: Any!) {
        candidatesWindow?.hide()
        if let client = currentClient, !lastPreedit.isEmpty {
            client.setMarkedText("", selectionRange: NSRange(location: 0, length: 0),
                                 replacementRange: NSRange(location: NSNotFound, length: 0))
        }
        lastPreedit = ""
        lastReply = nil
        super.deactivateServer(sender)
    }

    // MARK: key handling

    override func handle(_ event: NSEvent!, client sender: Any!) -> Bool {
        guard let event = event, let client = sender as? IMKTextInput else { return false }
        currentClient = client
        ensureEngine()
        // Modifier presses are key events to the engine: a lone Shift tap is
        // the schema's Chinese/English toggle, CapsLock the commit-code key.
        // Combinations (Cmd/Ctrl/Option shortcuts) and the shifted-letter
        // case keep flowing through the normal path below / to the app.
        if event.type == .flagsChanged {
            let conflicts = event.modifierFlags.intersection([.command, .control, .option])
            guard conflicts.isEmpty,
                  let repr = KeyCodeMapper.mapModifier(event),
                  let reply = engine?.key(repr: repr, mask: 0) else { return false }
            apply(reply, client)
            return reply.handled
        }
        guard event.type == .keyDown,
              let mapped = KeyCodeMapper.map(event),
              let reply = engine?.key(repr: mapped.repr, mask: mapped.mask) else {
            return false
        }
        apply(reply, client)
        return reply.handled
    }

    /// The client (app) asks for an immediate composition commit.
    override func commitComposition(_ sender: Any!) {
        if let client = currentClient,
           let reply = engine?.commit(),
           let text = reply.commit, !text.isEmpty {
            client.insertText(
                NSAttributedString(string: text),
                replacementRange: NSRange(location: NSNotFound, length: 0))
        }
        super.commitComposition(sender)
    }

    // MARK: candidate window integration

    override func candidates(_ sender: Any!) -> [Any]! {
        guard let reply = lastReply else { return [] }
        return reply.candidates.map { NSAttributedString(string: $0.text) }
    }

    override func candidateSelected(_ candidateString: NSAttributedString!) {
        guard let text = candidateString?.string,
              let reply = lastReply,
              let index = reply.candidates.firstIndex(where: { $0.text == text }),
              let selected = engine?.select(index) else { return }
        if let client = currentClient {
            apply(selected, client)
        }
    }

    override func candidateSelectionChanged(_ candidateString: NSAttributedString!) {
        // v1: no highlight-driven behavior yet
    }

    // MARK: internals

    private func ensureEngine() {
        if engine == nil { engine = EngineClient() }
        guard let engine = engine, !engine.isRunning else { return }
        let bundle = Bundle.main
        let shared = bundle.resourcePath! + "/rime"
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
            .first!.appendingPathComponent("JRime/rime", isDirectory: true)
        if !engine.start(sharedDir: shared, userDir: support.path, bundle: bundle) {
            engine.stop()
            // engine stays down; every key falls through to the system
        }
    }

    private func apply(_ reply: ServerReply, _ client: IMKTextInput) {
        lastReply = reply
        if let commit = reply.commit, !commit.isEmpty {
            client.insertText(
                NSAttributedString(string: commit),
                replacementRange: NSRange(location: NSNotFound, length: 0))
        }
        if reply.composing, !reply.preedit.isEmpty {
            let preedit = reply.preedit
            client.setMarkedText(
                NSAttributedString(string: preedit),
                selectionRange: NSRange(location: (preedit as NSString).length, length: 0),
                replacementRange: NSRange(location: NSNotFound, length: 0))
            showCandidates(reply)
        } else {
            if !lastPreedit.isEmpty {
                client.setMarkedText("", selectionRange: NSRange(location: 0, length: 0),
                                     replacementRange: NSRange(location: NSNotFound, length: 0))
            }
            candidatesWindow?.hide()
        }
        lastPreedit = reply.composing ? reply.preedit : ""
    }

    private func showCandidates(_ reply: ServerReply) {
        guard let window = candidatesWindow else { return }
        guard !reply.candidates.isEmpty else {
            window.hide()
            return
        }
        window.update() // re-queries candidates(_:) on this controller
        window.show(kIMKLocateCandidatesBelowHint)
    }
}
