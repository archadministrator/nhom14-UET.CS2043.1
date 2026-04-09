import java.util.concurrent.locks.*;

class Counter {
    int value = 0;
    ReentrantLock lock = new ReentrantLock();

    public void increment() {
        if (lock.tryLock()) {
            try {
                value++;
            } finally {
                lock.unlock();
            }
        }
    }
}

public class Bai9 {
    public static void main(String[] args) throws Exception {
        Counter c = new Counter();

        Runnable r = () -> {
            for (int i = 0; i < 10000; i++) c.increment();
        };

        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        Thread t3 = new Thread(r);
        Thread t4 = new Thread(r);

        t1.start(); t2.start(); t3.start(); t4.start();

        t1.join(); t2.join(); t3.join(); t4.join();

        System.out.println(c.value);
    }
}