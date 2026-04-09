import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class Order {
    String id;
    long processMs;

    public Order(String id, long processMs) {
        this.id = id;
        this.processMs = processMs;
    }
}

public class Bai5 {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        int m = sc.nextInt();
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            orders.add(new Order(sc.next(), sc.nextLong()));
        }

        ExecutorService es = Executors.newFixedThreadPool(4);
        List<Future<Boolean>> futures = new ArrayList<>();
        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger success = new AtomicInteger(0);

        for (Order o : orders) {
            Callable<Boolean> task = () -> {
                System.out.println("Start " + o.id);
                Thread.sleep(o.processMs);
                boolean ok = o.processMs <= 1500;
                if (ok) {
                    logs.add("DONE " + o.id);
                    success.incrementAndGet();
                } else {
                    logs.add("FAIL " + o.id);
                }
                return ok;
            };
            futures.add(es.submit(task));
        }

        for (Future<Boolean> f : futures) f.get();

        System.out.println("Success = " + success.get());
        for (String s : logs) System.out.println(s);

        es.shutdown();
    }
}