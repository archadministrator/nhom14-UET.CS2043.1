public class Bai8 {
    public static void main(String[] args) {
        
    }
}


interface Beverage {
    double cost();
    String getDescription();
}

// Concrete Class
class Coffee implements Beverage {
    private double cost = 20;
    private String description = "Simple coffee for simple personality";

    @Override
    public double cost(){
        return cost;
    }

    @Override
    public String getDescription(){
        return description;
    }
}


//Base Decorator
abstract class BeverageDecorator implements Beverage {
    
}