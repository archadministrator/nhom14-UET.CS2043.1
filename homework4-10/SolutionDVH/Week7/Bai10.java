class Worker implements Runnable {
    volatile boolean running = true;

    public void stop() {
        running = false;
    }

    public void run() {
        while (running) {
            System.out.println("Working...");
        }
    }
}

public class Bai10 {
    public static void main(String[] args) throws Exception {
        Worker w = new Worker();
        Thread t = new Thread(w);

        t.start();
        Thread.sleep(1000);
        w.stop();
        t.join();
    }
}