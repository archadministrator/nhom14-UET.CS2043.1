public class Logger {private static Logger instance =null;
    private Logger() {};
    public static Logger getInstance(){if(instance==null){instance=new Logger();}
    return instance;
    }
    void logInfo(String msg){System.out.println("[INFO]"+msg);}
    void logError(String msg){System.out.println("[ERROR]"+msg );}

    public static void main(String[] args) {
        Logger logger1 = Logger.getInstance();
        Logger logger2 = Logger.getInstance();
        if (logger1 == logger2) {
            System.out.println("Logger instances equal: true");
        }
        logger2.logInfo(" Application started");
        logger2.logInfo(" Processing data...");
        logger2.logError("Something went wrong");


    }}
