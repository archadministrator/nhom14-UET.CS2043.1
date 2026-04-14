interface Notification{void send(String msg);}
class EmailNotification implements Notification{public void send(String msg){System.out.println("Email gửi tin : "+msg);}}
class SmsNotification implements Notification{public void send(String msg){System.out.println("SMs gửi tin : "+msg);}}
public abstract class NotificationApp { public abstract Notification createNotification();
    public void notifyUser(String msg){Notification t1=createNotification();
    t1.send(msg);
    }
public static void main(String[]args){NotificationApp iphone=new EmailApp();
    iphone.notifyUser("Hey Siri");

    }


}
class EmailApp extends NotificationApp{public Notification createNotification(){return new EmailNotification();}}
class SmsApp extends NotificationApp{public Notification createNotification(){return new SmsNotification();}}
