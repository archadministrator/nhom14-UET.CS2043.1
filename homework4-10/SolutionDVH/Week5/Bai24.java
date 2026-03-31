import java.io.*;
import java.util.*;


public class Bai24 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Type in file path to write");
        String path = scanner.nextLine();

        String line = "";
        ObjectOutputStream oos = null;
        

        try {
            oos = new ObjectOutputStream(new FileOutputStream(path));

            
            while (!(line = scanner.nextLine()).equals("END")){
                String[] inputLine = line.trim().split("\\s+");
                oos.writeObject(new Student(inputLine[0],inputLine[1], Double.parseDouble(inputLine[2])));
            }
        } catch (FileNotFoundException e){
            System.out.println("Couldn't find file path");
        } catch (IOException e){
            System.out.println("I/O error.");
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) oos.close();
            } catch (IOException e) {
                System.out.println("I/O error: ");
                e.printStackTrace();
            }
        }


        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(path));
            while (true){
                Student s = (Student) ois.readObject();
                System.out.println(s);
            }
        } catch (FileNotFoundException e){
            System.out.println("Couldn't find file path");
        } catch (EOFException e){
            System.out.println("End of file reached.");
        } catch (ClassNotFoundException e){
            System.out.println("Couldn't find specific class");
        } catch (IOException e){
            System.out.println("I/O error: ");
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (IOException e) {
                System.out.println("I/O error: ");
                e.printStackTrace();
            }
        }
    }
}


class Student implements Serializable{
    private String id;
    private String name;
    private double gpa;

    public Student(String id, String name, double gpa){
        this.id = id;
        this.name = name;
        this.gpa = gpa;
    }
}