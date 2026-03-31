//                     <<abstract>>
//                     MediaItem
//         ---------------------------------
//         - id: String
//         - name: String
//         ---------------------------------
//         + getId(): String
//         + getInfo(): String (abstract)
//         ---------------------------------
//                          ▲
//             ┌────────────┴────────────┐
//             │                         │

//             Book                      DVD
//   ------------------------   ------------------------
//   - author: String           - director: String
//   - pages: int               - duration: int
//   ------------------------   ------------------------
//   + getInfo(): String        + getInfo(): String
//   ------------------------   ------------------------


//         LibrarySection<T extends MediaItem>
//         ------------------------------------
//         - library: List<T>
//         ------------------------------------
//         + addItem(item: T): void
//         + removeItem(id: String): void
//         + showDocumentList(): void
//         ------------------------------------

// Relationship:
// - Book        extends MediaItem
// - DVD         extends MediaItem
// - LibrarySection "has-a" List<T>
// - T bị ràng buộc: T extends MediaItem





import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Bai10{
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();

        LibrarySection<Book> bookLibrary = new LibrarySection<>();
        LibrarySection<DVD> dvdLibrary = new LibrarySection<>();

        for (int i = 0; i < n; i ++){
            String[] inputLine = scanner.nextLine().trim().split("\\s+");
            if (inputLine[0].equals("B")){
                bookLibrary.addItem(new Book(inputLine[1], inputLine[2], inputLine[3], Integer.parseInt(inputLine[4])));
            } else {
                dvdLibrary.addItem(new DVD(inputLine[1], inputLine[2], inputLine[3], Integer.parseInt(inputLine[4])));
            }
        }

        scanner.close();

        System.out.println("Book Section: ");
        bookLibrary.showDocumentList();
        System.out.println();

        System.out.println("DVD Section: ");
        dvdLibrary.showDocumentList();
    }
}


abstract class MediaItem {
    protected String id;
    protected String name;

    public MediaItem(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId(){return id;}

    abstract String getInfo();
}

class Book extends MediaItem {
    private String author;
    private int pages;

    public Book(String id, String name, String author, int pages){
        super(id, name);
        this.author = author;
        this.pages = pages;
    }

    @Override
    public String getInfo(){
        return name + " - " + author + " - " + pages; 
    }
}

class DVD extends MediaItem {
    private String director;
    private int duration;

    public DVD(String id, String name, String director, int duration){
        super(id, name);
        this.director = director;
        this.duration = duration;
    }

    @Override 
    public String getInfo(){
        return name + " - " + director + " - " + duration;
    }
}



class LibrarySection<T extends MediaItem> {
    private List<T> library = new ArrayList<>();

    public void addItem(T item){
        library.add(item);
    }

    public void removeItem(String id){
        library.removeIf(item -> item.getId().equals(id));
    }

    public void showDocumentList(){
        library.forEach(item -> System.out.println(item.getInfo()));
    }

}

