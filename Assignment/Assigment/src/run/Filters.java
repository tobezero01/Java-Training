package run;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

///
/// Chứa trạng thái các bộ lọc hiện đang áp dụng
/// levels - tập các level ( null = ko lọc)
/// from / to
/// msgContainsAny - từ khóa dùng để tìm kiếm message
/// serviceContains - ừ khóa để tìm kiếm cho Service
public class Filters {
    public Set<String> levels = null;
    public OffsetDateTime from = null;
    public OffsetDateTime to = null;
    List<String> msgContainsAny = null;
    String serviceContains = null;

    @Override
    public String toString() {
        return "LEVEL=" + (levels == null ? "ALL" : levels) +
                ", TIME[from=" + (from == null ? "-" : from) +
                ", to=" + (to == null ? "-" : to) + "]" +
                ", MSG_CONTAINS=" + (msgContainsAny == null || msgContainsAny.isEmpty() ? "(none)" : msgContainsAny) +
                ", SERVICE_CONTAINS=" + (serviceContains == null || serviceContains.isBlank() ? "(none)" : '"' + serviceContains + '"');
    }
}
