package run;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/// Thực thi
/// - Lọc bản ghi dựa trên filter
/// - Preview N dòng đầu tiên khớp bộ lọc (previewTop)
/// - Quét toàn bộ file và ghi các dòng khớp ra output (exportFiltered)
///
/// Đọc/ghi theo streaming
public class LogFilterEngine {
    private final LogParser parser;
    private final LogIO io;

    public LogFilterEngine(LogParser parser, LogIO io) {
        this.parser = parser;
        this.io = io;
    }

    /// Tiến hành thống kê sau một lần quét
    ///- total : tổng số dòng đã đọc
    ///- malformed : số dòng sai format (bị bỏ qua)
    ///- matched: số dòng khớp điều kiện
    public static class Stats {
        public long total;
        public long malformed;
        public long matched;
        public long elapsedMillis;
    }

    ///  ap dung cac dieu kien cua filter
    public boolean matches(LogRecord r, Filters f) {
        if (f.levels != null && !f.levels.contains(r.level)) return false;
        if (f.from != null && r.ts.isBefore(f.from)) return false;
        if (f.to != null && r.ts.isAfter(f.to)) return false;

        if (f.msgContainsAny != null && !f.msgContainsAny.isEmpty()) {
            String msg = r.message.toLowerCase(Locale.ROOT) ;
            boolean any = false;
            for (String key : f.msgContainsAny) {
                if (msg.contains(key)) { any = true ; break;}
            }
            if (!any) return false;
        }

        if (f.serviceContains != null && !f.serviceContains.isBlank()) {
            String sv = r.service.toLowerCase(Locale.ROOT);
            if (!sv.contains(f.serviceContains)) return false;
        }
        return true;
    }

    /// In ra stdout tối đa topN dòng đầu tiên KHỚP bộ lọc (không ghi file).
    public Stats previewTop(Filters f, int topN, PrintStream out) {
        Stats s = new Stats();
        try (BufferedReader br = io.openInputReader()) {
            String line ;
            while ((line = br.readLine()) != null) {
                s.total++;
                LogRecord rec = parser.parseLine(line);
                if (rec == null) {s.malformed++; continue;}
                if (matches(rec,f)) {
                    out.println(line);
                    s.matched++;
                    if (s.matched >= topN) break;
                }
            }

        } catch (IOException e) {
            out.println("Loi IO khi doc input: " + e.getMessage());
        }
        return s;
    }

    /// Quét toàn bộ input; mọi dòng KHỚP bộ lọc sẽ được ghi vào file output
    public Stats exportFiltered(Filters f, PrintStream out) {
        Stats s = new Stats();
        Path outPath = io.resolveOutputPath();

        Stopwatch sw = Stopwatch.startNew();

        try (BufferedReader br = io.openInputReader();
             BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

            String line;
            while ((line = br.readLine()) != null) {
                s.total++;
                LogRecord rec = parser.parseLine(line);
                if (rec == null) { s.malformed++; continue; }
                if (matches(rec, f)) {
                    bw.write(line);
                    bw.newLine();
                    s.matched++;
                }
            }
            bw.flush();
            out.println("Da xuat file: " + outPath.toAbsolutePath());
        } catch (IOException e) {
            out.println("Loi IO: " + e.getMessage());
        }

        s.elapsedMillis = sw.elapsedMillis();
        return s;
    }
}

