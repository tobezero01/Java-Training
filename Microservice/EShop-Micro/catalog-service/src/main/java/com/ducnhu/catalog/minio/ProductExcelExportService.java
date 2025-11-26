package com.ducnhu.catalog.minio;

import com.ducnhu.catalog.minio.dto.ExportResult;
import com.ducnhu.catalog.minio.dto.Row;
import com.ducnhu.catalog.minio.projection.ProductExportView;
import com.ducnhu.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ProductExcelExportService {
    private final ProductRepository productRepository;
    private final MinioUploader minioUploader;

    private static final int BATCH_SIZE = 2_000; // mỗi lần đọc DB
    private static final int QUEUE_CAP = 20_000; // han che ram
    private static final int FLUSH_EVERY = 2_000; // FLUSH row ra dia dinh ky
    private static final int WRITER_WINDOW = 1_000; // SXSSF window
    private static final int WORKERS = Math.max(2, Runtime.getRuntime().availableProcessors()/2);

    private static final Row POISON = new Row(-1, "", "", 0f, 0f, false, 0f, 0, "", ""); // sentinel kết thúc

    public ExportResult exportAllProductsToExcel() {
        try {
            // 1) Chuẩn bị MinIO bucket (idempotent)
            minioUploader.ensureBucket();

            // 2) Tạo file tạm (ghi streaming), đặt tên object mục tiêu
            File tmp = File.createTempFile("products-", ".xlsx");
            String objectName = "products/export-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".xlsx";

            //3). Tạo queue đẻ giới hạn phô hơp procedure/consume
            ArrayBlockingQueue<Row> queue = new ArrayBlockingQueue<>(QUEUE_CAP);

            // 4) Thread writer (1 luồng duy nhất sở hữu SXSSFWorkbook)
            ExecutorService writerExec = Executors.newSingleThreadExecutor(r -> new Thread(r, "excel-writer"));
            Future<?> writerFuture = writerExec.submit(() -> writeExcelStreaming(queue, tmp));

            // 5) Khởi động producers theo range id
            Integer minId = productRepository.minId();
            Integer maxId = productRepository.maxId();
            if (minId == null || maxId == null) {
                queue.put(POISON);
                writerFuture.get();
                minioUploader.uploadFile(tmp, objectName);
                String url = minioUploader.presign(objectName, 60);
                return new ExportResult(objectName, url, tmp.length());
            }

            int totalRange = (maxId - minId + 1);
            int segSize = Math.max(totalRange / WORKERS, 50_000); // mỗi worker xử lý tối thiểu 50k id
            ExecutorService producerPool = Executors.newFixedThreadPool(WORKERS, r -> new Thread(r, "db-producer"));
            CountDownLatch done = new CountDownLatch(WORKERS);
            for (int w=0; w<WORKERS; w++) {
                final int start = minId + w*segSize;
                final int end   = (w == WORKERS-1) ? maxId : Math.min(maxId, start + segSize - 1);
                if (start > end) { done.countDown(); continue; }

                producerPool.submit(() -> {
                    try {
                        scanRangeAndEnqueue(start, end, queue);
                    } finally {
                        done.countDown();
                    }
                });
            }
            done.await();
            queue.put(POISON);
            writerFuture.get(); // surface exception nếu writer lỗi

            // 7) Upload MinIO + presign URL
            minioUploader.uploadFile(tmp, objectName);
            String url = minioUploader.presign(objectName, 60);
            return new ExportResult(objectName, url, tmp.length());
        }
        catch (Exception e) {
            throw new RuntimeException("Export Excel failed", e);
        }
    }
    /** Đọc DB theo seek (id tăng), trong range [start..end], chunk BATCH_SIZE và đẩy Row vào queue */
    private void scanRangeAndEnqueue(int startInclusive, int endInclusive, ArrayBlockingQueue<Row> queue) {
        Integer afterId = startInclusive - 1; // id bắt đầu
        PageRequest page = PageRequest.of(0, BATCH_SIZE);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        while (true) {
            List<ProductExportView> batch = productRepository.scanAfterId(afterId, page);
            if (batch.isEmpty()) return;
            // Nếu phần tử đầu tiên đã vượt endInclusive -> dừng
            if (batch.get(0).getId() > endInclusive) return;

            // Chuẩn hóa Row + enqueue
            for (ProductExportView v : batch) {
                if (v.getId() > endInclusive) return;
                Row r = new Row(
                        v.getId(), v.getName(), v.getAlias(),
                        v.getPrice(), v.getDiscountPrice(),
                        v.getInStock(), v.getAverageRating(), v.getReviewCount(),
                        v.getCategoryName(),
                        v.getCreatedTime() == null ? "" : df.format(v.getCreatedTime())
                );
                try {
                    queue.put(r); // block nếu queue đầy => điều tiết RAM
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                afterId = v.getId();
            }
        }
    }

    /** Writer duy nhất, tạo SXSSFWorkbook và ghi tuần tự đến khi nhận POISON */
    private void writeExcelStreaming(ArrayBlockingQueue<Row> queue, File outFile) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(WRITER_WINDOW);
             FileOutputStream fos = new FileOutputStream(outFile)) {

            wb.setCompressTempFiles(true);               // giảm file tạm
            Sheet sheet = wb.createSheet("Products");    // tạo sheet
            ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();

            // ==== Styles cơ bản ====
            CellStyle header = wb.createCellStyle();
            Font bold = wb.createFont(); bold.setBold(true); header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            // ==== Header ====
            String[] cols = {"ID","Name","Alias","Price","Discount","In Stock","Avg Rating","Review Count","Category","Created Time"};
            Row headerRow = new Row(0, "", "", 0f,0f,false,0f,0,"","");
            org.apache.poi.ss.usermodel.Row h = sheet.createRow(0);
            for (int i=0;i<cols.length;i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(header);
            }

            // ==== Ghi data ====
            AtomicInteger rowNum = new AtomicInteger(1);
            int writtenSinceFlush = 0;

            while (true) {
                Row r = queue.take(); // chờ nếu chưa có
                if (r == POISON) break; // kết thúc

                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum.getAndIncrement());
                int col = 0;
                row.createCell(col++).setCellValue(s(r.id()));
                row.createCell(col++).setCellValue(s(r.name()));
                row.createCell(col++).setCellValue(s(r.alias()));
                row.createCell(col++).setCellValue(n(r.price()));
                row.createCell(col++).setCellValue(n(r.discount()));
                row.createCell(col++).setCellValue(r.inStock() ? "Y" : "N");
                row.createCell(col++).setCellValue(n(r.avgRating()));
                row.createCell(col++).setCellValue(n(r.reviewCount()));
                row.createCell(col++).setCellValue(s(r.categoryName()));
                row.createCell(col++).setCellValue(s(r.createdDate()));

                // flush định kỳ để giảm RAM
                if (++writtenSinceFlush >= FLUSH_EVERY) {
                    ((SXSSFSheet) sheet).flushRows(FLUSH_EVERY);
                    writtenSinceFlush = 0;
                }
            }

            // Auto-size cột chính (tốn I/O, cân nhắc nếu file cực lớn)
            for (int i=0;i<cols.length;i++) sheet.autoSizeColumn(i);

            wb.write(fos);  // ghi ra file
        } catch (Exception e) {
            throw new RuntimeException("Excel writer failed", e);
        }
    }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private static double n(Number n) { return n == null ? 0d : n.doubleValue(); }


}
