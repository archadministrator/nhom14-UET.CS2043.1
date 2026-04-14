interface Notifier{void send(String msg);}
class EmailNotifier implements Notifier{public void send(String msg){System.out.println("Email : "+msg);}}
abstract class NotifierDecorator implements Notifier{
    protected Notifier word;
    public NotifierDecorator(Notifier o){this.word=o;}
    public void send(String msg){word.send(msg);}


}
class SMSNotifier extends NotifierDecorator{public SMSNotifier(Notifier word){super(word);}
public void send(String msg){super.send(msg);
System.out.println("SMS"+msg);
}
}
class FacebookNotifier extends NotifierDecorator {
    public FacebookNotifier(Notifier notifier) {
        super(notifier);
    }
    public void send(String msg) {
        super.send(msg); // Gọi các kênh đã có trước đó
        System.out.println("Gửi Facebook: " + msg);
    }
}

public class Decorator {public static void main(String[] args) {
    Notifier myNotifier = new EmailNotifier();
    myNotifier = new FacebookNotifier(myNotifier);
    myNotifier = new SMSNotifier(myNotifier);
    myNotifier.send("Chào bạn, bạn có một thông báo mới!");
}
}
