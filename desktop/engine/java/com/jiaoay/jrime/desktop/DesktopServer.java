package com.jiaoay.jrime.desktop;

import com.jiaoay.jrime.core.CandidateProto;
import com.jiaoay.jrime.core.CommitProto;
import com.jiaoay.jrime.core.CompositionProto;
import com.jiaoay.jrime.core.ContextProto;
import com.jiaoay.jrime.core.Rime;
import com.jiaoay.jrime.core.RimeKeyEvent;
import com.jiaoay.jrime.core.StatusProto;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Desktop engine server — the shared core behind every platform shell (the
 * macOS IMK client today; a Linux ibus adapter and a Windows TSF server later
 * all speak this same line protocol over their transport).
 *
 * One engine per process. Requests, one per line on stdin:
 *   ready | k <repr> | sel <index> | commit | pg next | pg prev |
 *   opt <name> <bool> | exit
 * Responses: one JSON line per request on stdout, the same shape as the
 * harness trace (doc/harness-design.md §4) plus "handled". librime logs go to
 * stderr; stdout carries protocol lines only.
 */
public final class DesktopServer {

    private static final String VERSION = "1.0.0-desktop";

    public static void main(String[] args) throws Exception {
        String shared = null;
        String user = null;
        String schema = "flypy_xhfast";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--shared" -> shared = args[++i];
                case "--user" -> user = args[++i];
                case "--schema" -> schema = args[++i];
                default -> throw new IllegalArgumentException("unknown arg: " + args[i]);
            }
        }
        if (shared == null || user == null) {
            throw new IllegalArgumentException("--shared/--user are required");
        }

        // stdout carries the protocol only: every Java-side print (the JNI
        // notification callback logs, anything else) goes to stderr instead.
        // The protocol stream below is bound to FileDescriptor.out directly,
        // so this redirect does not affect responses.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.err),
                true, StandardCharsets.UTF_8));

        System.loadLibrary("rime_jni");
        Rime.startupRime(shared, user, VERSION);

        // Ready probe: the session must have mounted the expected schema before
        // the shell sends its first key, or responses would sample a dead engine.
        boolean ready = false;
        for (int i = 0; i < 150 && !ready; i++) {
            StatusProto st = Rime.getRimeStatus();
            if (schema.equals(st.schemaId)) {
                ready = true;
            } else {
                Thread.sleep(200);
            }
        }

        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        if (!ready) {
            out.println("{\"error\":\"engine never mounted schema " + json(schema)
                    + "\",\"messages\":" + json(String.join(", ", Rime.messages)) + "}");
            return;
        }

        // Idle watchdog: a shell process that dies without "exit" (crash, force
        // quit) must not leave an orphaned JVM holding the user dir forever.
        final long[] lastActivity = {System.currentTimeMillis()};
        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    return;
                }
                if (System.currentTimeMillis() - lastActivity[0] > 600_000) {
                    Rime.exitRime();
                    System.exit(0);
                }
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();

        String line;
        while ((line = in.readLine()) != null) {
            lastActivity[0] = System.currentTimeMillis();
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (line.equals("exit")) {
                    out.println(sample(line, true));
                    break;
                } else if (line.equals("ready")) {
                    out.println(sample(line, true));
                } else if (line.startsWith("k ")) {
                    RimeKeyEvent ev = RimeKeyEvent.parse(line.substring(2).strip());
                    boolean handled = Rime.processRimeKey(ev.keycode, ev.modifier);
                    out.println(sample(line, handled));
                } else if (line.startsWith("sel ")) {
                    boolean handled = Rime.selectRimeCandidate(
                            Integer.parseInt(line.substring(4).strip()), false);
                    out.println(sample(line, handled));
                } else if (line.equals("commit")) {
                    boolean handled = Rime.commitRimeComposition();
                    out.println(sample(line, handled));
                } else if (line.equals("pg next")) {
                    out.println(sample(line, Rime.changeRimeCandidatePage(false)));
                } else if (line.equals("pg prev")) {
                    out.println(sample(line, Rime.changeRimeCandidatePage(true)));
                } else if (line.startsWith("opt ")) {
                    String[] parts = line.split(" +");
                    Rime.setRimeOption(parts[1], Boolean.parseBoolean(parts[2]));
                    out.println(sample(line, true));
                } else {
                    out.println("{\"error\":\"unknown request: " + json(line) + "\"}");
                }
            } catch (Exception e) {
                out.println("{\"error\":\"" + json(String.valueOf(e)) + "\"}");
            }
        }
        Rime.exitRime();
    }

    /** Samples the engine state; identical shape to the harness trace minus step. */
    private static String sample(String op, boolean handled) {
        ContextProto ctx = Rime.getRimeContext();
        StatusProto st = Rime.getRimeStatus();
        CommitProto commit = Rime.getRimeCommit(); // drains the commit buffer
        CompositionProto comp = ctx.composition;
        String preedit = comp == null ? null : comp.preedit;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"op\":\"").append(json(op)).append('"')
                .append(",\"handled\":").append(handled)
                .append(",\"raw\":\"").append(json(Rime.getRimeRawInput())).append('"')
                .append(",\"preedit\":\"").append(json(preedit)).append('"')
                .append(",\"commit\":")
                .append(commit == null || commit.text == null
                        ? "null"
                        : "\"" + json(commit.text) + "\"")
                .append(",\"ascii\":").append(st.isAsciiMode)
                .append(",\"composing\":").append(st.isComposing)
                .append(",\"schema\":\"").append(json(st.schemaId)).append('"')
                .append(",\"cands\":[");
        CandidateProto[] cands = Rime.getRimeCandidates(0, 48);
        for (int i = 0; i < cands.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("[\"").append(json(cands[i].text)).append("\",\"")
                    .append(json(cands[i].comment)).append("\"]");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String json(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.toString();
    }
}
