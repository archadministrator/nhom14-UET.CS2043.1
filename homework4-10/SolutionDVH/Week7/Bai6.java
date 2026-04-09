import java.util.*;
import java.util.concurrent.*;

public class Bai6 {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        ExecutorService es = Executors.newFixedThreadPool(n);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int m = sc.nextInt();
            int[] arr = new int[m];
            for (int j = 0; j < m; j++) arr[j] = sc.nextInt();

            Callable<Integer> task = () -> {
                TreeSet<Integer> set = new TreeSet<>();
                for (int x : arr) set.add(x);
                if (set.size() < 2) return null;
                set.pollLast();
                return set.last();
            };
            futures.add(es.submit(task));
        }

        int sum = 0;
        for (int i = 0; i < n; i++) {
            Integer res = futures.get(i).get();
            if (res == null) {
                System.out.println("Array " + i + ": Not found");
            } else {
                System.out.println("Array " + i + ": second largest = " + res);
                sum += res;
            }
        }

        System.out.println("Sum = " + sum);
        es.shutdown();
    }
}