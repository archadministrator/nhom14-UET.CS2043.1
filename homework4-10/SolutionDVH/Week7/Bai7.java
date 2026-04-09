import java.util.*;
import java.util.concurrent.*;

public class Bai7 {
    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

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
                int count = 0;
                for (int x : arr) if (isPrime(x)) count++;
                return count;
            };
            futures.add(es.submit(task));
        }

        int max = -1;
        List<Integer> results = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int c = futures.get(i).get();
            results.add(c);
            System.out.println("Array " + i + ": " + c);
            max = Math.max(max, c);
        }

        for (int i = 0; i < n; i++) {
            if (results.get(i) == max) {
                System.out.println("Most primes: Array " + i + " with " + max + " primes");
            }
        }

        es.shutdown();
    }
}