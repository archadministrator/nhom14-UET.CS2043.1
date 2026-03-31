import java.util.*;

public class Bai7 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();
        List<Student> students = new ArrayList<>();

        for (int i = 0; i < n; i++){
            String[] inputLine = scanner.nextLine().trim().split("\\s+");
            students.add(new Student(inputLine[0],inputLine[1], Double.parseDouble(inputLine[2])));
        }

        scanner.close();

        students.removeIf(s -> s.getGPA() < 5.0);
        System.out.println("After removing GPA < 5.0:");
        students.forEach(s -> System.out.println(s.toString()));

        System.out.println();

        students.sort((s1,s2) -> s1.getName().compareTo(s2.getName()));
        System.out.println("After sorting by name: ");
        students.forEach(s -> System.out.println(s.toString()));

            // Lambda
        Operation<Double> add = (a, b) -> a + b;
        Operation<Double> sub = (a, b) -> a - b;
        Operation<Double> mul = (a, b) -> a * b;
        Operation<Double> div = (a, b) -> a / b;

    }
}


class Student {
    private String id;
    private String name;
    private double gpa;

    public Student(String id, String name, double gpa){
        this.id = id;
        this.name = name;
        this.gpa = gpa;
    }

    public void setGPA(double gpa){
        if (0 > gpa || 10 < gpa) {
            System.out.println("Invalid GPA!");
            return;
        }
        this.gpa = gpa;
    }

    

    public String getId(){return id;}
    public String getName(){return name;}
    public double getGPA(){return gpa;}

    @Override
    public String toString(){
        return id + " " + name + " " + gpa;
    }



}


interface Operation<T>{
    T execute(T a, T b);
}
