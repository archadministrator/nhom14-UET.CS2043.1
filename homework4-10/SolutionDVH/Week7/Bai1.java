class Task implements Runnable {
    String name;
    long durationMs;

    public Task(String name, long durationMs) {
        this.name = name;
        this.durationMs = durationMs;
    }

    public void run() {
        System.out.println("Start " + name);
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
        }
        System.out.println("End " + name);
    }
}

public class Bai1 {
    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(new Task("Task1", 1000));
        Thread t2 = new Thread(new Task("Task2", 1500));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("All tasks done.");
    }
}