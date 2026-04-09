class Report {
    private String title, content;
    public Report(String title, String content) {
        this.title = title; this.content = content;
    }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}

interface ReportFormatter {
    String format(Report r);
}

class JsonFormatter implements ReportFormatter {
    public String format(Report r) {
        return "{ \"title\": \"" + r.getTitle() + "\", \"content\": \"" + r.getContent() + "\" }";
    }
}

class XmlFormatter implements ReportFormatter {
    public String format(Report r) {
        return "<report><title>" + r.getTitle() + "</title><content>" +
                r.getContent() + "</content></report>";
    }
}

class ReportService {
    private ReportFormatter formatter;
    public ReportService(ReportFormatter formatter) {
        this.formatter = formatter;
    }
    public String export(Report r) {
        return formatter.format(r);
    }
}

public class Bai8 {
    public static void main(String[] args) {
        Report r = new Report("Test", "Hello");
        ReportService s = new ReportService(new JsonFormatter());
        System.out.println(s.export(r));
    }
}