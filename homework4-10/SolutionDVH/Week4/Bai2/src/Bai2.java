public class Bai2 {
    public static void main(String[] args) {
        Hero hero = new Hero();
        ((CanSwim) hero).swim();
        ((CanFight) hero).fight(); 
    }
}



interface CanFly {void fly();}
interface CanSwim {void swim();}
interface CanFight {void fight();}

class ActionCharacter implements CanFly, CanSwim, CanFight {  
    @Override 
    public void fly(){
        System.out.println("Character is flying");
    }

    @Override
    public void swim(){
        System.out.println("Character is swimming");
    }

    public void fight(){
        System.out.println("Boxing ...");
    }
} 


class Hero extends ActionCharacter {
    @Override 
    public void swim(){
        System.out.println("Hero is swimming");
    }

    @Override 
    public void fly(){
        System.out.println("Hero is flying");
    }
}

