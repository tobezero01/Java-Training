package Thread.Thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Demo {

    public static void main(String[] args) {
        var currentThread = Thread.currentThread();
        printThreadState(currentThread);

        CustomThread customThread = new CustomThread();
        customThread.start();

        Runnable myRunable = () -> {
            for (int i = 0; i < 8; i++) {
                System.out.print(" 2 ");
                try {
                    TimeUnit.MICROSECONDS.sleep(880);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread myThread2 = new Thread(myRunable);
        myThread2.start();

        for (int i = 0; i < 3; i++) {
            System.out.print(" 0 ");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void printThreadState(Thread currentThread) {
        System.out.println(currentThread.getId());
        System.out.println(currentThread.getClass().getName());
        System.out.println(currentThread.getName());
        System.out.println(currentThread.getPriority());
        System.out.println(currentThread.getThreadGroup());
        System.out.println("Thread is Alive " + currentThread.isAlive());
        System.out.println(currentThread.getState());
    }
     // Tạo Thread theo nhiều cách

    // Cách 1: Extends Thread
    static class HelloThread extends Thread {
        private final String name;

        HelloThread(String name) {this.name = name;}

        @Override
        public void run() {
            System.out.println("[HelloThread] Xin chào từ " + name + " | " + Thread.currentThread());
        }
    }

    // Cách 2: implements Runnable + truyền vào Thread
    static class HelloRunnable implements Runnable {
        private final String name;
        HelloRunnable(String name) { this.name = name; }
        @Override public void run() {
            System.out.println("[HelloRunnable] Xin chào từ " + name + " | " + Thread.currentThread());
        }
    }

    static void demoBasicThreads() throws InterruptedException {
        System.out.println("\n=== DEMO 1: Tạo thread cơ bản ===");
        Thread t1 = new HelloThread("T1");
        Thread t2 = new Thread(new HelloRunnable("R1"));
        t1.start();
        t2.start();

        // join: chờ 2 thread này kết thúc
        t1.join();
        t2.join();
        System.out.println("=> Basic threads đã xong.");
    }

    /* ========= Race condition & synchronized ====================== */

    static class UnsafeCounter {
        private int v  =0 ;
        void inc(){v++;}
        int get() {return v;}
    }

    static class SafeCounter {
        private int v = 0;
        synchronized void inc() { v++; }
        synchronized int get() { return v; }
    }

    static void demoRaceAndSync() throws InterruptedException {
        System.out.println("\n=== Race condition vs synchronized ===");
        final int THREADS = 8;
        final int INC_EACH = 100_000;

        UnsafeCounter uc = new UnsafeCounter();
        SafeCounter   sc = new SafeCounter();

        // Tạo 2 nhóm thread làm việc trên 2 counter
        List<Thread> group1 = new ArrayList<>(); // thao tác UnsafeCounter
        List<Thread> group2 = new ArrayList<>(); // thao tác SafeCounter

        for (int i = 0; i < THREADS; i++) {
            group1.add(new Thread(() -> {
                for (int k = 0; k < INC_EACH; k++) uc.inc();
            }));
            group2.add(new Thread(() -> {
                for (int k = 0; k < INC_EACH; k++) sc.inc();
            }));
        }

        long s1 = System.currentTimeMillis();
        for (Thread t : group1) t.start();
        for (Thread t : group1) t.join();
        long e1 = System.currentTimeMillis();

        long s2 = System.currentTimeMillis();
        for (Thread t : group2) t.start();
        for (Thread t : group2) t.join();
        long e2 = System.currentTimeMillis();

        int expected = THREADS * INC_EACH;
        System.out.printf("UnsafeCounter = %,d (expected %,d) | time=%dms%n", uc.get(), expected, (e1 - s1));
        System.out.printf("SafeCounter   = %,d (expected %,d) | time=%dms%n", sc.get(), expected, (e2 - s2));
        System.out.println("=> UnsafeCounter thường < expected do race; SafeCounter khớp expected nhờ synchronized.");
    }

    /* ==================== ExecutorService + Callable/Future ====================== */

    static void demoExecutorAndFuture() throws Exception {
        System.out.println("\n=== ExecutorService + Callable/Future ===");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            // 5 job giả lập CPU/IO nhẹ
            List<Callable<Integer>> jobs = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                int id = i;
                jobs.add(() -> {
                    // mô phỏng việc tính toán
                    Thread.sleep(50L * id);
                    System.out.println("  [pool] xong job " + id + " | " + Thread.currentThread());
                    return id * id;
                });
            }

            // submit & chờ xong tất cả
            List<Future<Integer>> futures = pool.invokeAll(jobs);
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> f : futures) {
                results.add(f.get()); // có thể dùng f.get(1, TimeUnit.SECONDS) để timeout
            }
            System.out.println("Kết quả: " + results);
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
