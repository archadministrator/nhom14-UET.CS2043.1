import java.util.*;
import java.util.concurrent.*;

public class Bai2 {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();

        int k = 4;
        ExecutorService es = Executors.newFixedThreadPool(k);
        List<Future<Integer>> futures = new ArrayList<>();

        int chunk = (n + k - 1) / k;
        for (int i = 0; i < n; i += chunk) {
            int start = i;
            int end = Math.min(i + chunk, n);

            Callable<Integer> task = () -> {
                int sum = 0;
                for (int j = start; j < end; j++) sum += arr[j];
                return sum;
            };
            futures.add(es.submit(task));
        }

        int total = 0;
        for (Future<Integer> f : futures) total += f.get();

        System.out.println(total);
        es.shutdown();
    }
}