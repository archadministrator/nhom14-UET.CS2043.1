class Logger{private static Logger instance=null;
private Logger(){};
public static Logger getInstance(){if(instance==null){instance=new Logger();}
return instance;}

}
interface Export{void connect();}
class PdfExport implements Export{public void connect(){System.out.println("Xuất bằng PDF");}}
class ExcelExport implements Export{public void connect(){System.out.println("Xuất bằng Excel");}}
abstract class EFactory{public abstract Export createExport(String type);}
class PDfFactory extends EFactory{public Export createExport(String type){return new PdfExport();}}
class ExcelFactory extends EFactory{public Export createExport(String type){return new ExcelExport();}}
class OldPlayer{void playFile(String name){System.out.println("CHơi thôi "+name);}}
interface Player{void play(String name);}
class PlayerAdapter implements Player{
    private OldPlayer t1;
    PlayerAdapter(OldPlayer OldPlayer){this.t1=OldPlayer;}
    public void play(String name){t1.playFile(name);}
}
interface Prototype {
    Prototype clone();
}

class Configuration implements Prototype {
    private String setting;
    public Configuration(String setting) { this.setting = setting; }
    public void setSetting(String setting) { this.setting = setting; }
    public Configuration clone() {
        return new Configuration(this.setting);
    }
    public String toString() {
        return "Config[Setting=" + setting + "]";
    }
}
public class DesignPattern  {
    public static void main(String[] args) {
        Logger logger1 = Logger.getInstance();
        Logger logger2 = Logger.getInstance();
        PDfFactory t1=new PDfFactory();
        Export pdf = t1.createExport("PDF");
        pdf.connect();
        OldPlayer oldStyle = new OldPlayer();
        Player modernPlayer = new PlayerAdapter(oldStyle);
        modernPlayer.play("song.mp3");
        Configuration originalConfig = new Configuration("Default Theme");
        Configuration clonedConfig = originalConfig.clone();
        clonedConfig.setSetting("Dark Mode");
        System.out.println("Original: " + originalConfig);
        System.out.println("Clone (edited): " + clonedConfig);
    }
}

