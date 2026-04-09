import java.util.*;
public class Bai6 {
    public static void main(String[] args) {

        // root
        Folder root = new Folder("root");

        // docs
        Folder docs = new Folder("docs");

        FileItem a = new FileItem("a.txt", 12);
        FileItem b = new FileItem("b.txt", 8);

        // build tree
        docs.add(a);
        docs.add(b);

        Shortcut shortcut = new Shortcut("a-shortcut", a);
        docs.add(shortcut);

        root.add(docs);
        root.add(new FileItem("readme.md", 4));

        // print
        root.print("");
    }
}


interface FileSystemItem {
    void print(String indent);
    String getPath();
    void setParent(Folder parent);
}


abstract class AbstractFileSystemItem implements FileSystemItem {
    protected String name;
    protected Folder parent;

    public AbstractFileSystemItem(String name){
        this.name = name;
    }

    @Override 
    public void setParent(Folder parent){
        this.parent = parent;
    }

    @Override 
    public String getPath(){
        if (parent == null){
            return "/" + name;
        } 
        return parent.getPath() + "/" + name;
    }

}


class FileItem extends AbstractFileSystemItem {
    private int size;

    public FileItem(String name, int size){
        super(name);
        this.size = size;
    }

    @Override
    public void print(String indent){
        System.out.println(indent + "File: " + name + "(" + size + "KB)");
    }
}   


class Shortcut extends AbstractFileSystemItem {
    private FileSystemItem target;

    public Shortcut(String name, FileSystemItem target) {
        super(name);
        this.target = target;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Shortcut: " + name + " -> " + target.getPath());
    }
}


class Folder extends AbstractFileSystemItem {
    private List<FileSystemItem> children = new ArrayList<>();

    public Folder(String name) {
        super(name);
    }

    public void add(FileSystemItem item) {
        item.setParent(this); 
        children.add(item);
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Folder: " + name);
        for (FileSystemItem child : children) {
            child.print(indent + "  ");
        }
    }
}


