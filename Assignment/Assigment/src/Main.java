import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String INPUT_FILE = "logInput.txt";
    private static final String OUTPUT_FILE = "logOutput.txt";

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^\\[(.+?)]\\s*\\[(\\w+)]\\s*\\[([^\\]]+)]\\s*-\\s*(.*)$"
    );

    private static class Filters {
        Set<String> levels = null;           // null = không lọc
        OffsetDateTime from = null;          // null = không đặt mốc
        OffsetDateTime to = null;            // null = không đặt mốc
        List<String> containsAny = null;     // null/empty = không lọc
    }

    // Bản ghi log đã parse
    private static class LogRecord {
        final OffsetDateTime ts;
        final String level;
        final String service;
        final String message;

        LogRecord(OffsetDateTime ts, String level, String service, String message) {
            this.ts = ts;
            this.level = level;
            this.service = service;
            this.message = message;
        }
    }

    private static void printMenu(Filters f) {
        System.out.println("============== LOG FILTER MENU ==============");
        System.out.println("File vao : " + INPUT_FILE);
        System.out.println("File ra  : " + OUTPUT_FILE);
        System.out.println("---------------------------------------------");
        System.out.println("Bo loc hien tai:");
        System.out.println("  1) Level       : " + (f.levels == null ? "ALL (khong loc)" : f.levels));
        System.out.println("  2) Time range  : from=" + (f.from == null ? "-" : f.from)
                + "  to=" + (f.to == null ? "-" : f.to));
        System.out.println("  3) Message has : " + (f.containsAny == null || f.containsAny.isEmpty() ? "(khong loc)" : f.containsAny));
        System.out.println("---------------------------------------------");
        System.out.println("  4) Xuat ket qua -> " + OUTPUT_FILE);
        System.out.println("  5) Thoat");
        System.out.println("=============================================");
    }

    // trả về 1 log obj từ việc xử lỹ chuỗi
    private static LogRecord parseLine(String line) {
        Matcher m = LOG_PATTERN.matcher(line);
        if (!m.matches()) return null;
        String tsStr = m.group(1);
        String level = m.group(2).toUpperCase(Locale.ROOT);
        String service = m.group(3);
        String message = m.group(4);

        OffsetDateTime ts = parseOdt(tsStr);

        return new LogRecord(ts, level, service, message);
    }

    private static boolean matches(LogRecord r, Filters f) {
        // Level
        if (f.levels != null && !f.levels.contains(r.level)) return false;

        // Time range (inclusive)
        if (f.from != null && r.ts.isBefore(f.from)) return false;
        if (f.to   != null && r.ts.isAfter(f.to))     return false;

        // Message contains (any)
        if (f.containsAny != null && !f.containsAny.isEmpty()) {
            String msgLower = r.message.toLowerCase(Locale.ROOT);
            boolean any = false;
            for (String key : f.containsAny) {
                if (msgLower.contains(key)) { any = true; break; }
            }
            if (!any) return false;
        }
        return true;
    }

    private static OffsetDateTime parseOdt(String s) {
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}