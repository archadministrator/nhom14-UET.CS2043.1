import java.util.*;


public class Bai4b {
    public static void main(String[] args) {
        List<String> sections = new ArrayList<>();
        sections.add("Intro");
        sections.add("Outtro");
        ReportTemplate originReport = new ReportTemplate("Blublbublub", "Rose", sections);

        ReportTemplate copy1Report = originReport.clone();
        copy1Report.setTitle("Blebblebbleb");

        ReportTemplate copy2Report = originReport.clone(); 
        copy2Report.setTitle("Blabblabblab");

        originReport.displayInfo();
        copy1Report.displayInfo();
        copy2Report.displayInfo();
    }
}




class ReportTemplate implements Cloneable{
    private String title;
    private String footer;
    private List<String> sections;

    public ReportTemplate(String title, String footer, List<String> sections){
        this.title = title;
        this.footer = footer;
        this.sections = sections;
    }

    public void setTitle(String title){
        this.title = title;
    }

    @Override
    public ReportTemplate clone(){
        List<String> cloneSections = new ArrayList<>(this.sections);
        return new ReportTemplate(this.title, this.footer, cloneSections);
    }

    public void displayInfo(){
        System.out.println("Title: " + title);
        System.out.println("Footer: " + footer);
        System.out.print("Sections: ");
        sections.forEach(a -> System.out.print(a + ", "));
        System.out.println();
    }
}