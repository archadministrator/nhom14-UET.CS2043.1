import java.util.*;

public class Bai15 {
    public static void main(String[] args) {
        System.out.println("------------- Library 1 ------------");
        Library1 lib1 = new Library1();
        lib1.addBook(new Book("1", "Java", "Author A", 2020));
        lib1.addBook(new Book("2", "Python", "Author B", 2019));
        lib1.addBook(new Book("3", "C++", "Author C", 2018));
        lib1.addBook(new Book("4", "JavaScript", "Author D", 2021));
        lib1.addBook(new Book("5", "Data Structures", "Author E", 2022));
        lib1.findBook("10");
        lib1.findBook("2");
        lib1.removeBook("2");
        lib1.displayLibrary();
        System.out.println();

        System.out.println("------------- Library 2 ------------");
        Library2 lib2 = new Library2();
        lib1.addBook(new Book("1", "Java", "Author A", 2020));
        lib1.addBook(new Book("2", "Python", "Author B", 2019));
        lib1.addBook(new Book("3", "C++", "Author C", 2018));
        lib1.addBook(new Book("4", "JavaScript", "Author D", 2021));
        lib1.addBook(new Book("5", "Data Structures", "Author E", 2022));
        lib1.findBook("10");
        lib1.findBook("2");
        lib1.removeBook("2");
        lib1.displayLibrary();
        System.out.println();


        System.out.println("------------- Library 1 ------------");
        Library3 lib3 = new Library3();
        lib1.addBook(new Book("1", "Java", "Author A", 2020));
        lib1.addBook(new Book("2", "Python", "Author B", 2019));
        lib1.addBook(new Book("3", "C++", "Author C", 2018));
        lib1.addBook(new Book("4", "JavaScript", "Author D", 2021));
        lib1.addBook(new Book("5", "Data Structures", "Author E", 2022));
        lib1.findBook("10");
        lib1.findBook("2");
        lib1.removeBook("2");
        lib1.displayLibrary();
        System.out.println();

    }
}





class Book {
    private String id;
    private String title;
    private String author;
    private int year;

    public Book(String id, String title, String author, int year){
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
    }

    public String getId(){return id;}

    public void showInfo(){
        System.out.println("Book's information: ");
        System.out.println("Id: " + id);
        System.out.println("Title: " + title);
        System.out.println("Author: " + author);
        System.out.println("Year: " + year);
    }

}


abstract class Library {
    abstract void addBook(Book b);
    abstract void findBook(String id);
    abstract void removeBook(String id);
    abstract void displayLibrary();
}

class Library1 extends Library {
    List<Book> books = new ArrayList<>();
    
    @Override
    public void addBook(Book b){
        books.add(b);
    }

    @Override
    public void findBook(String id){
        for (Book b: books){
            if (b.getId().equals(id)){
                b.showInfo();
                return;
            }
        }
        System.out.println("No book with id " + id);

    }

    @Override
    public void removeBook(String id){
        books.removeIf(b -> b.getId().equals(id));
    }

    @Override
    public void displayLibrary(){
        System.out.println("Library: ");
        books.forEach(b -> b.showInfo());
    }

}

class Library2 extends Library {
    HashMap<String, Book> books = new HashMap<>();

    @Override
    public void addBook(Book b){
        books.put(b.getId(), b);
    }

    @Override
    public void findBook(String id){
        if (!books.containsKey(id)){
            System.out.println("No book with id " + id);
        } else {
            Book b = books.get(id);
            System.out.println("Found book: ");
            b.showInfo();
        }
    }

    @Override 
    public void removeBook(String id){
        if (!books.containsKey(id)){
            System.out.println("No book with id " + id);
        } else {
            books.remove(id);
        }
    }

    @Override 
    public void displayLibrary(){
        for (Map.Entry<String, Book> entry: books.entrySet()){
            Book b = entry.getValue();
            b.showInfo();
        }
    }
}

class Library3 extends Library {
    TreeMap<String, Book> books = new TreeMap<>();

    @Override
    public void addBook(Book b){
        books.put(b.getId(), b);
    }

    @Override 
    public void findBook(String id){
        if (!books.containsKey(id)){
            System.out.println("No book with id " + id);
        } else {
            Book b = books.get(id);
            System.out.println("Found book: ");
            b.showInfo();
        }
    }

    @Override 
    public void removeBook(String id){
        if (!books.containsKey(id)){
            System.out.println("No book with id " + id);
        } else {
            books.remove(id);
        }
    }

    @Override 
    public void displayLibrary(){
        for (Map.Entry<String, Book> entry: books.entrySet()){
            Book b = entry.getValue();
            b.showInfo();
        }
    }
}