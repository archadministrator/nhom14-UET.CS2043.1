class Logger {
    private static volatile Logger instance;

    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public void logInfo(String msg) {
        System.out.println("[INFO] " + msg);
    }

    public void logError(String msg) {
        System.out.println("[ERROR] " + msg);
    }
}

public class Bai10 {
    public static void main(String[] args) {
        Logger l1 = Logger.getInstance();
        Logger l2 = Logger.getInstance();

        System.out.println("Logger instances equal: " + (l1 == l2));

        l1.logInfo("Application started");
        l2.logInfo("Processing data...");
        l1.logError("Something went wrong");
    }
}

