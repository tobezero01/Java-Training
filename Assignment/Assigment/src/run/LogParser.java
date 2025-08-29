package run;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Lớp này chuyển 1 dòng text theo format
///   thành đối tượng run.LogRecord.
public class LogParser {

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^\\[(.+?)]\\s*\\[(\\w+)]\\s*\\[([^\\]]+)]\\s*-\\s*(.*)$"
    );

    /// @param line 1 dòng log dạng text
    /// @return run.LogRecord nếu parse thành công, hoặc null nếu dòng sai format/hay sai timestamp
    public LogRecord parseLine(String line) {
        Matcher m = LOG_PATTERN.matcher(line);
        if (!m.matches()) return null;

        String tsStr = m.group(1);
        String level = m.group(2).toUpperCase(Locale.ROOT);
        String service = m.group(3);
        String message = m.group(4);

        try {
            OffsetDateTime ts = OffsetDateTime.parse(tsStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return new LogRecord(ts, level, service, message);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
