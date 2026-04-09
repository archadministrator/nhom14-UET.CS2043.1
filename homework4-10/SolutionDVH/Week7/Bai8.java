import java.util.*;
import java.util.concurrent.*;

public class Bai8 {
    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        ExecutorService stage1 = Executors.newFixedThreadPool(n);
        ExecutorService stage2 = Executors.newFixedThreadPool(n);

        List<Future<List<Integer>>> f1 = new ArrayList<>();
        List<Future<Integer>> f2 = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int idx = i;
            int m = sc.nextInt();
            int[] arr = new int[m];
            for (int j = 0; j < m; j++) arr[j] = sc.nextInt();

            Future<List<Integer>> future1 = stage1.submit(() -> {
                List<Integer> primes = new ArrayList<>();
                for (int x : arr) if (isPrime(x)) primes.add(x);
                System.out.println("Stage 1 - Array " + idx + ": " + primes);
                return primes;
            });
            f1.add(future1);
        }

        for (int i = 0; i < n; i++) {
            int idx = i;
            Future<Integer> future2 = stage2.submit(() -> {
                List<Integer> primes = f1.get(idx).get();
                int sum = 0;
                if (primes.size() % 2 == 0) {
                    for (int x : primes) sum += x * x;
                    System.out.println("Stage 2 - Array " + idx + ": sum of squares = " + sum);
                } else {
                    for (int x : primes) sum += x * x * x;
                    System.out.println("Stage 2 - Array " + idx + ": sum of cubes = " + sum);
                }
                return sum;
            });
            f2.add(future2);
        }

        int total = 0;
        for (Future<Integer> f : f2) total += f.get();

        System.out.println("Total = " + total);

        stage1.shutdown();
        stage2.shutdown();
    }
}