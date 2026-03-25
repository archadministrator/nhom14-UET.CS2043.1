
public class Bai1 {
    public static void main(String[] args) {
        Circle circle = new Circle(10,10);
        circle.moveTo(20,20);
    }
}


abstract class Shape {
    protected int x;
    protected int y;

    public Shape(int x, int y){
        this.x = x;
        this.y = y;
    }

    abstract public void draw();
    abstract public void erase();
    public  void update(int newX, int newY){
        System.out.println("Point's coordinates has been changed from (" + x + "," + y + ") to (" + newX + "," + newY + ")" );
    }

    public void moveTo(int newX, int newY){
        erase();
        update(newX, newY);
        draw();
    }
}

class Circle extends Shape {
    public Circle(int x, int y){
        super(x, y);
        System.out.println("Circle created at (" + x + "," + y + ")");
    }

    @Override 
    public void draw(){
        System.out.println("Circle has just been drawn at (" + x + "," + y + ")");
    }

    @Override 
    public void erase(){
        System.out.println("Circle has just been erased at (" + x + "," + y + ")");
    }
}

class Square extends Shape {
    public Square(int x, int y){
        super(x, y);
        System.out.println("Square created at (" + x + "," + y + ")");
    }

    @Override 
    public void draw(){
        System.out.println("Square has just been drawn at (" + x + "," + ")");
    }

    @Override
    public void erase(){
        System.out.println("Square has just been erased at (" + x + "," + ")");
    }
}

