// Component: Notifier
// Concrete Class: Notifier Decorator
// Decorator: EmailNotifier, FacebookNotifier, SMSNotifier, ZaloNotifier


public class Bai7 {
    public static void main(String[] args) {
        Notifier notifier = new EmailNotifier();

        notifier = new SMSNotifier(new FacebookNotifier(new ZaloNotifier (notifier)));

        notifier.send("Hello, New Account!");
    }
}



// Component
interface Notifier {
    void send(String msg);
}

// Concrete Component
class EmailNotifier implements Notifier {
    @Override 
    public void send(String msg){
        System.out.println("Notifier from email: " + msg);
    }
}

// Decorator
abstract class NotifierDecorator implements Notifier {
    /////////////////////////
    protected Notifier wrappee;

    public NotifierDecorator(Notifier notifier){
        this.wrappee = notifier;
    }
    /////////////////////////
    
    @Override 
    public void send(String msg){
        wrappee.send(msg);
    }
}

// Concrete Decorator
class SMSNotifier extends NotifierDecorator {
    public SMSNotifier(Notifier notifier){
        super(notifier);
    }

    @Override
    public void send(String msg){
        super.send(msg);
        System.out.println("Notifier from SMS: " + msg);
    }
}

class FacebookNotifier extends NotifierDecorator {
    public FacebookNotifier(Notifier notifier){
        super(notifier);
    }
    
    @Override
    public void send(String msg){
        super.send(msg);
        System.out.println("Notifier from Facebook: " + msg);
    }
}

class ZaloNotifier extends NotifierDecorator {
    public ZaloNotifier(Notifier notifier){
        super(notifier);
    }

    @Override 
    public void send(String msg){
        super.send(msg);
        System.out.println("Notifier from Zalo: " + msg);
    }
}