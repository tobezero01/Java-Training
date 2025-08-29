package run;

import java.time.OffsetDateTime;

///
/// Mô hình (DTO) đại diện cho 1 dòng log đã parse thành các trường
///
public class LogRecord {
    public final OffsetDateTime ts;
    public final String level;
    public final String service;
    public final String message;

    public LogRecord(OffsetDateTime ts, String level, String service, String message) {
        this.ts = ts;
        this.level = level;
        this.service = service;
        this.message = message;
    }
}
