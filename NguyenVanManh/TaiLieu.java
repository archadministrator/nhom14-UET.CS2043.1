import java.util.ArrayList;
abstract class FileSystemItem{public abstract void  print(String indent);
    public String getPath() {
        if (parent == null) return "/" + name;
        String parentPath = parent.getPath();
        return (parentPath.equals("/") ? "" : parentPath) + "/" + name;
    }

    protected String name;
    protected Folder parent;

    public FileSystemItem(String name) {
        this.name = name;
    }
    public void setParent(Folder parent) {
        this.parent = parent;
    }
}
class FileItem extends FileSystemItem{String name;int size;public FileItem(String name,int size){super(name);this.name=name;this.size=size;};public void print(String indent){System.out.println(indent+" File:" +name+ "("+size+"KB)");}}
class Shortcut extends FileSystemItem {FileSystemItem target;
public Shortcut(String name,FileSystemItem target){super(name);this.target=target;}
public void print(String indent){System.out.println(indent + " Shortcut: " + name + " -> " + target.getPath());}
;}
class Folder extends FileSystemItem{public Folder(String name){super(name);}
ArrayList<FileSystemItem>kho=new ArrayList<>();
    public void print(String indent){System.out.println(indent+" Folder "+name);
for(FileSystemItem s:kho){s.print(indent+" ");}}
    public void add(FileSystemItem item) {
        kho.add(item);
        item.setParent(this);
    }
}




public class TaiLieu {
    public static void main(String[] args) {
        Folder root = new Folder("root");

        Folder docs = new Folder("docs");
        FileItem report = new FileItem("report.pdf", 150);
        docs.add(report);
        docs.add(new FileItem("data.txt", 45));


        Folder images = new Folder("images");
        FileItem logo = new FileItem("logo.png", 200);
        images.add(logo);


        Shortcut shortcutToReport = new Shortcut("my_report_link", report);
        root.add(docs);
        root.add(images);
        root.add(shortcutToReport);

        System.out.println("HỆ THỐNG FILE:");
        root.print("");
    }
}

