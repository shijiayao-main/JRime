import com.jiaoay.jrime.core.CandidateProto;
import com.jiaoay.jrime.core.CommitProto;
import com.jiaoay.jrime.core.CompositionProto;
import com.jiaoay.jrime.core.ContextProto;
import com.jiaoay.jrime.core.Rime;
import com.jiaoay.jrime.core.RimeKeyEvent;
import com.jiaoay.jrime.core.StatusProto;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Harness H1 engine driver (doc/harness-design.md §3/§4): replays one corpus
 * case against librime through the desktop JNI bridge, sampling a JSONL trace
 * after every step. One engine per process — cases are isolated by fresh user
 * dirs; the shared staging dir is prepared by run_cases.py.
 *
 * Step tokens: k:<repr> (librime key repr via RimeKeyEvent.parse), sel:<idx>,
 * commit, pg:next, pg:prev, opt:<name>=<bool>.
 */
public class Replay {

    public static void main(String[] args) throws Exception {
        String shared = null;
        String user = null;
        String out = null;
        String schema = "flypy_xhfast";
        List<String> steps = List.of();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--shared" -> shared = args[++i];
                case "--user" -> user = args[++i];
                case "--out" -> out = args[++i];
                case "--schema" -> schema = args[++i];
                case "--steps" -> {
                    String raw = args[++i];
                    steps = raw.isBlank() ? List.of() : List.of(raw.split(","));
                }
                default -> throw new IllegalArgumentException("unknown arg: " + args[i]);
            }
        }
        if (shared == null || user == null || out == null) {
            throw new IllegalArgumentException("--shared/--user/--out are required");
        }

        System.loadLibrary("rime_jni");
        Rime.startupRime(shared, user, "1.0.0-harness");

        // Ready probe: the session must have mounted the expected schema before
        // the first key goes in, or the trace would sample a dead engine.
        boolean ready = false;
        for (int i = 0; i < 150 && !ready; i++) {
            StatusProto st = Rime.getRimeStatus();
            if (schema.equals(st.schemaId)) {
                ready = true;
            } else {
                Thread.sleep(200);
            }
        }
        if (!ready) {
            System.err.println("engine never mounted schema " + schema
                    + "; messages=" + Rime.messages);
            System.exit(2);
        }

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(out), StandardCharsets.UTF_8))) {
            sample(w, 0, "start", true);
            int n = 1;
            for (String op : steps) {
                boolean handled = apply(op.strip());
                sample(w, n, op.strip(), handled);
                n++;
            }
            sample(w, n, "end", true);
        }
        Rime.exitRime();
        System.out.println("REPLAY OK");
    }

    /** Applies one op; returns whether the engine consumed it (false = the app layer would act). */
    private static boolean apply(String op) {
        if (op.startsWith("k:")) {
            RimeKeyEvent ev = RimeKeyEvent.parse(op.substring(2));
            return Rime.processRimeKey(ev.keycode, ev.modifier);
        }
        if (op.startsWith("sel:")) {
            return Rime.selectRimeCandidate(Integer.parseInt(op.substring(4)), false);
        }
        if (op.equals("commit")) {
            return Rime.commitRimeComposition();
        }
        if (op.equals("pg:next")) {
            return Rime.changeRimeCandidatePage(false);
        }
        if (op.equals("pg:prev")) {
            return Rime.changeRimeCandidatePage(true);
        }
        if (op.startsWith("opt:")) {
            int eq = op.indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("opt needs name=value: " + op);
            Rime.setRimeOption(op.substring(4, eq), Boolean.parseBoolean(op.substring(eq + 1)));
            return true;
        }
        throw new IllegalArgumentException("unknown op: " + op);
    }

    private static void sample(PrintWriter w, int step, String op, boolean handled) {
        ContextProto ctx = Rime.getRimeContext();
        StatusProto st = Rime.getRimeStatus();
        CommitProto commit = Rime.getRimeCommit(); // drains the commit buffer
        CompositionProto comp = ctx.composition;
        String preedit = comp == null ? null : comp.preedit;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"step\":").append(step)
                .append(",\"op\":\"").append(json(op)).append('"')
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
        w.println(sb.toString());
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
