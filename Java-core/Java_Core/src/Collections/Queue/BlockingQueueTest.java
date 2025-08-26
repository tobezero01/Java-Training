package Collections.Queue;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockingQueueTest {
    record Job(int id, String producer) {}

    // Poison pill (sentinel) để dừng consumer
    static final Job POISON = new Job(-1, "POISON");

    public static void main(String[] args) throws InterruptedException {
        // Queue giới hạn dung lượng -> có back-pressure
        // fairness=true để xếp hàng công bằng khi nhiều thread chờ (đổi lại có overhead nhỏ)
        int capacity = 8;
        BlockingQueue<Job> queue = new ArrayBlockingQueue(capacity, true);

        int PRODUCERS = 2;
        int CONSUMERS = 3;
        int JOBS_PER_PRODUCER = 20;

        CountDownLatch producersDone = new CountDownLatch(PRODUCERS);
        List<Thread> threads = new ArrayList<>();

        // ===== Producers =====
        for (int p = 1; p <= PRODUCERS; p++) {
            final int id = p;
            Thread t = new Thread(() -> {
                try {
                    for (int i = 1; i <= JOBS_PER_PRODUCER; i++) {
                        Job job = new Job(i, "P" + id);
                        queue.put(job); // chặn nếu đầy
                        // mô phỏng tốc độ sinh việc
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    // Khôi phục cờ interrupt và thoát gọn
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                    System.out.printf("[Producer P%d] Done%n", id);
                }
            }, "Producer-" + id);
            threads.add(t);
            t.start();
        }

        // ===== Consumers =====
        for (int c = 1; c <= CONSUMERS; c++) {
            final int id = c;
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        Job job = queue.take(); // chặn nếu rỗng
                        if (job == POISON) {
                            System.out.printf("  [Consumer C%d] Got POISON -> exit%n", id);
                            break;
                        }
                        // Xử lý công việc
                        process(job, id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.printf("  [Consumer C%d] Stopped%n", id);
                }
            }, "Consumer-" + id);
            threads.add(t);
            t.start();
        }

        // ===== Phối hợp dừng hệ thống =====
        producersDone.await();                 // đợi producers xong
        for (int i = 0; i < CONSUMERS; i++) {  // gửi đúng số POISON = số consumers
            queue.put(POISON);
        }

        // Đợi tất cả thread kết thúc gọn gàng
        for (Thread t : threads) t.join();
        System.out.println("All done.");
    }

    // Giả lập xử lý mất thời gian
    static void process(Job job, int consumerId) throws InterruptedException {
        System.out.printf("  [Consumer C%d] handle %s#%d%n",
                consumerId, job.producer(), job.id());
        Thread.sleep(50); // mô phỏng I/O/CPU
    }
}
