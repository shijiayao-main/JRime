import Foundation

struct EngineCandidate {
    let text: String
    let comment: String
}

/// One decoded response line from the engine server; the same shape as the
/// harness trace minus step (doc/harness-design.md §4).
struct ServerReply {
    let obj: [String: Any]

    var handled: Bool { obj["handled"] as? Bool ?? false }
    var raw: String { obj["raw"] as? String ?? "" }
    var preedit: String { obj["preedit"] as? String ?? "" }
    var composing: Bool { obj["composing"] as? Bool ?? false }
    var commit: String? { obj["commit"] as? String }
    var schemaId: String { obj["schema"] as? String ?? "" }
    /// Protocol version asserted by the shell: the shell supports protocol 1
    /// and accepts any lower value (versions only ever increment).
    var proto: Int { obj["proto"] as? Int ?? 0 }
    var engineVersion: String { obj["engine"] as? String ?? "" }
    var candidates: [EngineCandidate] {
        (obj["cands"] as? [[Any]])?.compactMap { pair in
            guard let texts = pair as? [String], texts.count == 2 else { return nil }
            return EngineCandidate(text: texts[0], comment: texts[1])
        } ?? []
    }
}

/// Speaks the desktop engine line protocol to a child JVM running
/// DesktopServer. One client owns one engine process; the JVM dies with the
/// "exit" request or the server-side idle watchdog cleans it up.
final class EngineClient {
    private var process: Process?
    private var inHandle: FileHandle?
    private var outHandle: FileHandle?
    private var buffer = [UInt8]()

    var isRunning: Bool { process?.isRunning ?? false }

    @discardableResult
    func start(sharedDir: String, userDir: String, bundle: Bundle) -> Bool {
        stop()
        do {
            try FileManager.default.createDirectory(
                atPath: userDir, withIntermediateDirectories: true)
        } catch {
            return false
        }
        let java = bundle.resourcePath! + "/runtime/bin/java" // jlink image layout
        let p = Process()
        p.executableURL = URL(fileURLWithPath: java)
        p.arguments = [
            "-Djava.library.path=" + bundle.resourcePath! + "/lib",
            "-cp", bundle.resourcePath! + "/engine",
            "com.jiaoay.jrime.desktop.DesktopServer",
            "--shared", sharedDir,
            "--user", userDir,
        ]
        let inPipe = Pipe()
        let outPipe = Pipe()
        p.standardInput = inPipe
        p.standardOutput = outPipe
        p.standardError = FileHandle.nullDevice
        do {
            try p.run()
        } catch {
            return false
        }
        process = p
        inHandle = inPipe.fileHandleForWriting
        outHandle = outPipe.fileHandleForReading
        buffer = []

        // Warm the engine before the first key: block until the schema mounts.
        // Handshake: the shell supports protocol versions <= 1; a server
        // advertising a higher version is incompatible (reject, do not type).
        let deadline = Date().addingTimeInterval(30)
        while Date() < deadline {
            if !p.isRunning { return false }
            if let r = exchange("ready"), !r.schemaId.isEmpty {
                if r.proto > 1 { return false }
                return true
            }
            Thread.sleep(forTimeInterval: 0.1)
        }
        return false
    }

    func stop() {
        if let p = process, p.isRunning {
            inHandle?.write(Data("exit\n".utf8))
            let deadline = Date().addingTimeInterval(3)
            while p.isRunning && Date() < deadline {
                Thread.sleep(forTimeInterval: 0.05)
            }
            if p.isRunning { p.terminate() }
        }
        process = nil
        inHandle = nil
        outHandle = nil
        buffer = []
    }

    func key(repr: String, mask: Int) -> ServerReply? { exchange("k \(repr)") }

    func select(_ index: Int) -> ServerReply? { exchange("sel \(index)") }

    func commit() -> ServerReply? { exchange("commit") }

    func setPage(backward: Bool) -> ServerReply? {
        exchange(backward ? "pg prev" : "pg next")
    }

    func setOption(_ name: String, _ value: Bool) -> ServerReply? {
        exchange("opt \(name) \(value)")
    }

    private func exchange(_ line: String) -> ServerReply? {
        guard let handle = inHandle, process?.isRunning == true else { return nil }
        handle.write(Data((line + "\n").utf8))
        let deadline = Date().addingTimeInterval(10)
        while Date() < deadline {
            if let reply = nextReply() { return reply }
            if process?.isRunning != true { return nil }
            Thread.sleep(forTimeInterval: 0.02)
        }
        return nil
    }

    private func nextReply() -> ServerReply? {
        guard let out = outHandle else { return nil }
        let chunk = out.availableData
        if chunk.isEmpty { return nil } // EOF: engine process died
        buffer += chunk
        while let nl = buffer.firstIndex(of: 0x0A) {
            let lineBytes = Array(buffer[0..<nl])
            buffer.removeFirst(nl + 1)
            guard let line = String(bytes: lineBytes, encoding: .utf8) else { continue }
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            // tolerate stray non-protocol lines on stdout
            guard trimmed.hasPrefix("{"),
                  let data = trimmed.data(using: .utf8),
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { continue }
            return ServerReply(obj: obj)
        }
        return nil
    }
}
