import java.util.Scanner;

public class Bai3 {
    public static void main(String[] args){
        //Input handling
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();

        Employee[] employees = new Employee[n];
        for(int i = 0; i < n; i++){
            Employee employee;
            String[] inputLine = scanner.nextLine().trim().split("\\s+");
            if(inputLine[0].equals("O")){
                employee = new OfficeWorker(inputLine[1],inputLine[2], Double.parseDouble(inputLine[3]));
            } else {
                employee = new Technician(inputLine[1], inputLine[2], Double.parseDouble(inputLine[3]), Integer.parseInt(inputLine[4]));
            }
            employees[i] = employee;
        }

        for(Employee employee: employees){
            System.out.println(employee.getName() + " - Pay: " + employee.calculatePay());
            employee.work();
        }
    }
}


interface IWorkable {void work();}

abstract class Employee implements IWorkable{
    protected String id;
    protected String name;
    protected double baseSalary;

    public Employee(String id, String name, double baseSalary){
        this.id = id;
        this.name = name;
        this.baseSalary = baseSalary;
    }

    public String getName(){return name;}

    abstract double calculatePay();
}

class OfficeWorker extends Employee {
    public OfficeWorker(String id, String name, double baseSalary){
        super(id, name, baseSalary);
    }
    public double calculatePay(){
        return baseSalary;
    }
    
    @Override 
    public void work(){
        System.out.println("Soạn thảo văn bản");
    }
}

class Technician extends  Employee {
    protected int overtimeHours;

    public Technician(String id, String name, double baseSalary, int overtimeHours){
        super(id, name, baseSalary);
        this.overtimeHours = overtimeHours;
    }

    @Override
    public double calculatePay(){
        return baseSalary + overtimeHours * 20000;
    }

    @Override 
    public void work(){
        System.out.println("Lắp đặt thiết bị");
    }
}

