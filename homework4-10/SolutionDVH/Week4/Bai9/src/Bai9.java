//                  +----------------------+
//                  |       Product       |
//                  +----------------------+
//                  | - id: String        |
//                  | - name: String      |
//                  +----------------------+
//                  | + getInfo(): String |
//                  +----------+-----------+
//                             |
//         -----------------------------------------
//         |                                       |
// +---------------------+           +------------------------+
// |        Food         |           |      Electronic        |
// +---------------------+           +------------------------+
// | - expiryDate: String|           | - warrantyMonths: int  |
// +---------------------+           +------------------------+
// | + getInfo(): String |           | + getInfo(): String    |
// +---------------------+           +------------------------+


//                  +--------------------------------------+
//                  |      Warehouse<T extends Product>    |
//                  +--------------------------------------+
//                  | - items: List<T>                     |
//                  +--------------------------------------+
//                  | + addItem(T item): void              |
//                  | + removeItem(T item): void           |
//                  | + printInventory(): void             |
//                  +--------------------------------------+

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Bai9{
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();

        Warehouse<Food> foodWarehouse = new Warehouse<>();
        Warehouse<Electronic> elecWareHouse = new Warehouse<>();

        for (int i = 0; i < n; i++){
            String[] inputLine = scanner.nextLine().trim().split("\\s+");
            if(inputLine[0].equals("E")){
                elecWareHouse.addItem(new Electronic(inputLine[1], inputLine[2], inputLine[3]));
            }
            else {
                foodWarehouse.addItem(new Food(inputLine[1], inputLine[2], inputLine[3]));
            }
        }

        System.out.println("Food Warehouse: ");
        foodWarehouse.printInventory();

        System.out.println();
        System.out.println("Electronic Warehouse: ");
        elecWareHouse.printInventory();

    }
}

abstract class Product {
    protected String id;
    protected String name;

    public Product(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getId(){
        return id;
    }

    abstract String getInfo();
}


class Food extends Product {
    private String expiryDate;

    public Food(String id, String name, String expiryDate){
        super(id, name);
        this.expiryDate = expiryDate;
    }   

    @Override
    public String getInfo(){
        return name + " - " + expiryDate;
    }
}

class Electronic extends Product {
    private String warrantyMonths;

    public Electronic(String id, String name, String warrantyMonths){
        super(id, name);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getInfo(){
        return name + " - " + warrantyMonths;
    }
}



class Warehouse<T extends Product> {
    private List<T> items = new ArrayList<>();

    public void addItem(T item){
        items.add(item);
    }

    public void removeItem(String id){
        items.removeIf(item -> item.getId().equals(id));
    }

    public void printInventory(){
        items.forEach(item -> System.out.println(item.getInfo()));
    }
}