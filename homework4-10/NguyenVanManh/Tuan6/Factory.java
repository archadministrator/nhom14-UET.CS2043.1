import java.util.Scanner;
interface Button{void render();}
interface Checkbox{void render();}
interface UIFactory{
    Button createButton();
    Checkbox createCheckbox();
}
class WindowsButton implements Button {public void render(){System.out.println("Đã cài đặt widow button");};}
class WindowCheckBox implements Checkbox {public void render(){System.out.println("Đã cài đặt window checkbox");} }
class MacButton implements Button {public void render(){System.out.println("Đã cài đặt Mac button");}}
class MacCheckbox implements Checkbox{public void render(){System.out.println("Đã cài đặt Mac checkbox");}}
class WindowsFactory implements UIFactory{public Button createButton(){return new WindowsButton();}
public Checkbox createCheckbox(){return new WindowCheckBox();}
}
class MacFactory implements UIFactory{public Button createButton(){return new MacButton();}
public Checkbox createCheckbox(){return new MacCheckbox();}
}

public class Factory { public static void main(String[]args){Scanner sc=new Scanner(System.in);UIFactory t1;
String word=sc.next();
if (word.equalsIgnoreCase("mac")){ t1=new MacFactory();t1.createButton().render();t1.createCheckbox().render();}
else if(word.equalsIgnoreCase("window")){t1=new WindowsFactory();t1.createCheckbox().render();t1.createButton().render();}
}
}
