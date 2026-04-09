import java.util.Scanner;

public class Bai3 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose configuration - mac/win?: ");
        String type = scanner.nextLine();

        UIFactory factory = null;
        if (type.equals("win")){
            factory = new WindowsFactory();
        } else if (type.equals("mac")){
            factory = new MacFactory();
        } else {
            System.out.println("Invalid input");
        }
        scanner.close();

        Button button = factory.createButton();
        Checkbox checkbox = factory.createCheckbox();

        button.render();
        checkbox.render();
    }
}



interface Button {void render();}
interface Checkbox {void render();}

interface UIFactory{
    Button createButton();
    Checkbox createCheckbox();
}


// WINDOWS factory
class WindowsButton implements Button {
    public void render(){
        System.out.println("Rendered Windows button");
    }
}
class WindowsCheckbox implements Checkbox {
    public void render(){
        System.out.println("Rendered Windows checkbox");
    }
}

class WindowsFactory implements UIFactory {
    public Button createButton(){
        return new WindowsButton();
    }
    public Checkbox createCheckbox(){
        return new WindowsCheckbox();
    }
}


//Mac factory 
class MacButton implements Button {
    public void render(){
        System.out.println("Rendered Mac button");
    }
}
class MacCheckbox implements Checkbox {
    public void render(){
        System.out.println("Rendered Mac checkbox");
    }
}
class MacFactory implements UIFactory {
    public Button createButton(){
        return new MacButton();
    }
    public Checkbox createCheckbox(){
        return new MacCheckbox();
    }
}


// Ubuntu factory
class UbuntuButton implements Button {
    public void render(){
        System.out.println("Rendered ubuntu button");
    }
}
class UbuntuCheckbox implements Checkbox {
    public void render(){
        System.out.println("Rendered ubuntu checkbox");
    }
}
class UbuntuFactory implements UIFactory {
    public Button createButton(){
        return new UbuntuButton();
    }
    public Checkbox createCheckbox(){
        return new UbuntuCheckbox();
    }
}