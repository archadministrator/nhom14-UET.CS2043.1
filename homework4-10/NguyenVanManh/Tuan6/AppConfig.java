public class AppConfig {
    private String AppName;
    private String Version;
    private String LogLevel;
    private AppConfig(){}
    private static AppConfig instance=null;
public static AppConfig getInstance(){if (instance==null){instance=new AppConfig();}
    return instance;

}
public static void main(String[]args){
    Thread t1 = new Thread(() -> {
        System.out.println("Luồng 1 - Hash: " + AppConfig.getInstance().hashCode());
    });
    Thread t2 = new Thread(() -> {
        System.out.println("Luồng 2 - Hash: " + AppConfig.getInstance().hashCode());
    });
    t1.start();
    t2.start();

}
}
