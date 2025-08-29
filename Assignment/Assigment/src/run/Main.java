package run;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    private static final String INPUT_NAME  = "logInput.txt";
    private static final String OUTPUT_NAME = "logOutput.txt";

    private static final int PREVIEW_TOP_N = 20;

    public static void main(String[] args) {
        Filters filters = new Filters();
        LogParser parser = new LogParser();
        LogIO io = new LogIO(INPUT_NAME, OUTPUT_NAME);
        LogFilterEngine engine = new LogFilterEngine(parser, io);
        Scanner sc = new Scanner(System.in);

        try {
            io.openInputReader().close();
        } catch (Exception e) {
            System.err.println("Khong tim thay '" + INPUT_NAME + "' o working dir: " +
                    System.getProperty("user.dir") + " hoac trong classpath (cung package).");
            return;
        }

        while (true) {
            printMenu(filters, io);
            System.out.print("Chon (1-7): ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> setLevelFilter(sc, filters);
                case "2" -> setTimeRangeFilter(sc, filters);
                case "3" -> setMsgContainsFilter(sc, filters);
                case "4" -> setServiceContainsFilter(sc, filters);
                case "5" -> doPreview(engine, filters);
                case "6" -> doExport(engine, filters);
                case "7" -> { System.out.println("Da thoat."); return; }
                default -> System.out.println("Lua chon khong hop le. Thu lai.");
            }
            System.out.println();
        }
    }

    /// Nhập chuỗi levels (ví dụ "INFO,WARN,ERROR")
    private static void setLevelFilter(Scanner sc, Filters f) {
        System.out.println("Nhap LEVEL (vd: INFO,WARN,ERROR). ALL/De trong = khong loc.");
        System.out.print("Levels: ");
        String line = sc.nextLine().trim();
        if (line.isEmpty() || line.equalsIgnoreCase("ALL")) {
            f.levels = null;
            System.out.println(">> LEVEL = ALL");
            return;
        }
        Set<String> s = new LinkedHashSet<>();
        for (String p : line.split(",")) {
            String lv = p.trim().toUpperCase(Locale.ROOT);
            if (!lv.isEmpty()) s.add(lv);
        }

        f.levels = s.isEmpty() ? null : s;
        System.out.println(">> LEVEL = " + (f.levels == null ? "ALL" : f.levels));
    }

    /// Nhập mốc thời gian from/to theo ISO-8601 + timezone
    private static void setTimeRangeFilter(Scanner sc, Filters f) {
        System.out.println("Nhap ISO 8601 co timezone. VD: 2025-08-27T10:00:00+07:00 (de trong = bo moc).");
        System.out.print("From: ");
        String fs = sc.nextLine().trim();
        System.out.print("To  : ");
        String ts = sc.nextLine().trim();

        f.from = parseOdtOrNull(fs);
        f.to   = parseOdtOrNull(ts);

        if (f.from != null && f.to != null && f.from.isAfter(f.to)) {
            System.out.println(">> Canh bao: From > To. Hoan doi lai.");
            var tmp = f.from; f.from = f.to; f.to = tmp;
        }
        System.out.println(">> TIME RANGE set: from=" + (f.from==null?"-":f.from) + " to=" + (f.to==null?"-":f.to));
    }

    /// Nhập danh sách từ khóa cho message, phân tách bởi dấu phẩy
    private static void setMsgContainsFilter(Scanner sc, Filters f) {
        System.out.println("Nhap tu khoa message (OR, phan tach boi dau phay, khong phan biet hoa/thuong).");
        System.out.print("Keywords: ");
        String kw = sc.nextLine().trim();
        if (kw.isEmpty()) {
            f.msgContainsAny = null;
            System.out.println(">> MESSAGE CONTAINS = (none)");
            return;
        }
        List<String> keys = new ArrayList<>();
        for (String p : kw.split(",")) {
            String s = p.trim().toLowerCase(Locale.ROOT);
            if (!s.isEmpty()) keys.add(s);
        }
        f.msgContainsAny = keys.isEmpty() ? null : keys;
        System.out.println(">> MESSAGE CONTAINS = " + f.msgContainsAny);
    }

    /// Nhập chuỗi cần tìm trong phần service
    private static void setServiceContainsFilter(Scanner sc, Filters f) {
        System.out.println("Nhap chuoi can tim trong [service] (khong phan biet hoa/thuong). De trong = bo loc.");
        System.out.print("Service contains: ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) {
            f.serviceContains = null;
            System.out.println(">> SERVICE CONTAINS = (none)");
        } else {
            f.serviceContains = s.toLowerCase(Locale.ROOT);
            System.out.println(">> SERVICE CONTAINS = \"" + f.serviceContains + "\"");
        }
    }

    private static void doPreview(LogFilterEngine engine, Filters f) {
        System.out.println(">> Preview " + PREVIEW_TOP_N + " dong dau tien theo bo loc:");
        LogFilterEngine.Stats s = engine.previewTop(f, PREVIEW_TOP_N, System.out);
        System.out.println("---------------------------------------------");
        System.out.println("Tong so dong doc   : " + s.total);
        System.out.println("So dong sai format : " + s.malformed);
        System.out.println("So dong da hien thi: " + s.matched);
        System.out.println("---------------------------------------------");
    }

    private static void doExport(LogFilterEngine engine, Filters f) {
        System.out.println(">> Dang xuat ket qua...");
        LogFilterEngine.Stats s = engine.exportFiltered(f, System.out);

        // Tính throughput an toàn (tránh chia 0)
        double seconds = Math.max(0.001, s.elapsedMillis / 1000.0);
        double procPerSec = s.total / seconds;    // tốc độ xử lý (dòng/giây)
        double writPerSec = s.matched / seconds;  // tốc độ ghi (dòng/giây)

        System.out.println("---------------------------------------------");
        System.out.println("Tong so dong doc      : " + s.total);
        System.out.println("So dong sai format    : " + s.malformed);
        System.out.println("So dong ghi vao output: " + s.matched);
        System.out.println(String.format(
                "Thoi gian xuat        : %d ms (%.3f s)", s.elapsedMillis, seconds));
        System.out.println(String.format(
                "Throughput xu ly      : %.0f dong/giay", procPerSec));
        System.out.println(String.format(
                "Throughput ghi        : %.0f dong/giay", writPerSec));
        System.out.println("---------------------------------------------");
    }

    ///  Thanh menu dieu huong
    private static void printMenu(Filters f, LogIO io) {
        System.out.println("------------------------------------------------");
        System.out.println("Bo loc dang ap dung: " + f);
        System.out.println("------------------------------------------------");
        System.out.println("  1) Dat LEVEL (vd: INFO,WARN,ERROR | ALL)");
        System.out.println("  2) Dat TIME RANGE (ISO-8601 + TZ)");
        System.out.println("  3) Dat MESSAGE CONTAINS (OR, icase)");
        System.out.println("  4) Dat SERVICE CONTAINS (icase)");
        System.out.println("  5) Xem " + PREVIEW_TOP_N + " dong dau tien theo bo loc");
        System.out.println("  6) Xuat ket qua -> " + OUTPUT_NAME);
        System.out.println("  7) Thoat");
        System.out.println("================================================");
    }

    /// Tiện ích parse chuỗi thời gian ISO-8601 + TZ -> OffsetDateTime
    private static OffsetDateTime parseOdtOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.out.println(">> Khong parse duoc thoi gian: " + s + " (bo qua)");
            return null;
        }
    }

}
