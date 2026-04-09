// Logger cần SINGLETON

// Các đối tượng export như pdfExport, excelExport cần FACTORY METHOD và ABSTRACT FACTORY

// Player sử dụng ADAPTER để thích ứng với OldPlayer

// Tạo bản sao sử dụng PROTOTYPE


public class Bai5 {
    public static void main(String[] args) {
        Logger logger = Logger.getInstance();
        logger.displayInfo();

        PdfFactory pdf = new PdfFactory();
        ExcelFactory excel = new ExcelFactory();
        
        pdf.createExport();
        excel.createExport();

        
    }
}

// SINGLETON
class Logger {
    private static volatile Logger instance;

    private String appName;
    private String logLevel;
    private String logFilePath;

    private Logger(){
        appName = "Testing App";
        logLevel = "DEBUG";
        logFilePath = "Unknown";
        System.out.println("Logger created");
    }

    public static Logger getInstance(){
        if (instance == null){
            synchronized (Logger.class) {
                if (instance == null){
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public void displayInfo(){
        System.out.println("App name: " + appName);
        System.out.println("Log level: " + logLevel);
        System.out.println("Log file path: " + logFilePath);
    }
}


// FACTORY METHOD, ABSTRACT FACTORY
abstract class Export {
    protected String fileName;
    protected String data;
    protected String author;

    abstract void export();

    public void setFileName(String fileName){this.fileName = fileName;}

    public void setData(String data){this.data = data;}
}

class PdfExport extends Export {
    @Override 
    public void export(){
        System.out.println("Exporting PDF file...");
        System.out.println("File: " + fileName);
        System.out.println("Data: " + data);
        System.out.println("Author: " + author);
        System.out.println("... Done!");
    }
}

class ExcelExport extends Export {
    @Override
    public void export(){
        System.out.println("Exporting Excel  sheet...");
        System.out.println("File: " + fileName);
        System.out.println("Data: " + data);
        System.out.println("Author: " + author);
        System.out.println("... Done!");
    }
}


abstract class ExportFactory {  
    // For each factory
    protected abstract Export createExport();

    // For all factory
    public void runExport(String fileName, String data){
        Export export = createExport();
        export.setFileName(fileName);
        export.setData(data);
        export.export();
    }
}

class PdfFactory extends ExportFactory {
    @Override
    protected Export createExport(){
        return new PdfExport();
    }
}

class ExcelFactory extends ExportFactory {
    @Override
    protected Export createExport(){
        return new ExcelExport();
    }
}






// ADAPTER

class OldPlayer {
    protected void playFile(String name){
        System.out.println("Playing file " + name + "like an old man");
    };
}

interface Player{
    void play(String name);
}

class PlayerAdapter implements Player {
    private OldPlayer oldPlayer;
    public PlayerAdapter(OldPlayer oldPlayer){
        this.oldPlayer = oldPlayer;
    }

    @Override
    public void play(String name){
        oldPlayer.playFile(name);
    }
}


// Prototype
class Configuration implements Cloneable {
    private String type;
    private String id;
    private String version;

    public Configuration(String type, String id, String version){
        this.type = type;
        this.id = id;
        this.version = version;
    }

    @Override
    public Configuration clone(){
        return new Configuration(this.type, this.id, this.version);
    }
}

