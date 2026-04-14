
interface ReportFormater{public String format(Report Data);}
class Report{private String title;private String content;
public Report(String title,String content){this.title=title;this.content=content;}
    public String getTitle(){return title;}
    public String getContent(){return content;}
}
class JsonFormatter implements ReportFormater{public String format(Report Data){return "Json Title: "+Data.getTitle()+" Content; "+Data.getContent();}}
class XmlFormatter implements ReportFormater{public String format(Report Data){return "Xml Title: "+Data.getTitle()+" Content; "+Data.getContent();}}
public class ReportService {
    private ReportFormater a;
    public ReportService(ReportFormater b){this.a=b;}
    public String export(Report Data){return a.format(Data);}
public static void main(String[]args){
Report a=new Report("7 chu lun","hello");
JsonFormatter t2=new JsonFormatter();
ReportService t1=new ReportService(t2);
System.out.println(
t1.export(a));
ReportService t3=new ReportService(new XmlFormatter());
System.out.println(t3.export(a));
}

}