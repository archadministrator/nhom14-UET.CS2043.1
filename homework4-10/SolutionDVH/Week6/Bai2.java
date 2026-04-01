    public class Bai2 {
        public static void main(String[] args) {
            NotificationApp app = new EmailApp();
            NotificationApp app2 = new SmsApp();

            app.notifyUser("Hello from email app");
            app2.notifyUser("Hello from SMS app");
        }
    }


    interface Notification {void send(String message);}

    // Root
    abstract class NotificationApp {
        public void notifyUser(String message){
            Notification notification = createNotification();
            notification.send(message);
        }

        protected abstract Notification createNotification(); //
    }

    class EmailNotification implements Notification {
        @Override
        public void send(String message){
            System.out.println("Send email: " + message);
        }
    }

    class SmsNotification implements Notification {
        @Override
        public void send(String message){
            System.out.println("Send SMS: " + message);
        }
    }





    // Fruit 
    class EmailApp extends NotificationApp {
        @Override
        protected Notification createNotification(){
            return new EmailNotification();
        }
    }

    class SmsApp extends NotificationApp {
        @Override
        protected Notification createNotification(){
            return new SmsNotification();
        }
    }