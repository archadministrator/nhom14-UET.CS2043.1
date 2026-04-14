import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

interface Sorter{int[]sort(int[]arr);}
class LegacySorter{public int[] quickSort(int[]arr ){Arrays.sort(arr);return arr;}}
class SortedAdapter implements Sorter{public int[] sort(int[]arr){LegacySorter sc=new LegacySorter();return sc.quickSort(arr);}}
public class Adapter {public static void main(String[]args){int[]arr={5,2,3,1,9};SortedAdapter t1=new SortedAdapter();t1.sort(arr);
System.out.println("danh sách : "+ Arrays.toString(arr));
}
}

 class ReportTemplate implements Cloneable {private String title;private String footer;private List<String> section;
     public ReportTemplate(String title, String footer, List<String> sections) {
         this.title = title;
         this.footer = footer;
         this.section = sections;}
     public void setTitle(String title){this.title=title;}
     public List<String> get(){return section;}
protected Object clone() throws CloneNotSupportedException{
         ReportTemplate cloned=(ReportTemplate) super.clone();
        cloned.section=new ArrayList<>(this.section);
        return cloned;
}
public String toString(){return "Title : "+title+" |footer : "+footer+" |section :"+section;}
public static void main(String[]args){try{
    List<String> defaultSections = new ArrayList<>();
    defaultSections.add("Hello");
    defaultSections.add("gutbai");

    ReportTemplate goc = new ReportTemplate("Chaobuoisang", "2222", defaultSections);
    ReportTemplate report1 = (ReportTemplate) goc.clone();
    report1.setTitle("vuasutu");
    report1.get().add("aumaigut");
    ReportTemplate report2 = (ReportTemplate) goc.clone();
    report2.setTitle("Security  2026");
    System.out.println("ORIGINAL: " + goc);
    System.out.println("COPY 1:   " + report1);
    System.out.println("COPY 2:   " + report2);

} catch (CloneNotSupportedException e) {
    e.printStackTrace();


}}
}


