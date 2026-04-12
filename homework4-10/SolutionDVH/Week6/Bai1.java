public class Bai1 {
    public static void main(String[] args) {
        Runnable task = () -> {
            AppConfig config = AppConfig.getInstance();
            System.out.println(Thread.currentThread().getName() + ": " + config.hashCode());
        };

        Thread t1 = new Thread(task, "Thread 1");
        Thread t2 = new Thread(task, "Thread 2");

        t1.start();
        t2.start();
    }
}


class AppConfig {
    public static volatile AppConfig instance;

    private String appName;
    private String version;
    private String logLevel;

    private AppConfig(){
        this.appName = "App Name";
        this.version = "1.21.11";
        this.logLevel = "CEBR";
    }

    // Singleton core
    public static AppConfig getInstance(){
        if (instance == null) { 
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }
}




