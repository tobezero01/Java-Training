package Collections.Queue;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Demo {
    public static void main(String[] args) throws Exception{
        // FIFO bằng ArrayDeque
        Deque<String> q = new ArrayDeque<>();
        q.offer("A"); q.offer("B"); q.offer("C");
        System.out.println(q.poll()); // A
        System.out.println(q.peek()); // B

        // PriorityQueue (min-heap)
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pq.offer(5); pq.offer(1); pq.offer(3);
        System.out.println(pq.poll()); // 1 (ưu tiên nhỏ nhất)

        // BlockingQueue producer-consumer (bounded)
        BlockingQueue<String> bq = new ArrayBlockingQueue<>(2);
        Thread producer = new Thread(() -> {
            try { for (var s : List.of("x","y","z","END")) bq.put(s); } catch (InterruptedException e) { }
        });
        Thread consumer = new Thread(() -> {
            try {
                String v;
                while (!"END".equals(v = bq.take())) System.out.println("consumed: " + v);
            } catch (InterruptedException e) { }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }

}
