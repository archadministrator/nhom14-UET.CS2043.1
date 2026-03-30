import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Stack;

public class Bai12 {
    public static void main(String[] args) {
        //Bat dau phien
        MessageHistory messageHistory = new MessageHistory();
        CustomerQueue customerQueue = new CustomerQueue();

        //Khach A den
        Customer a = new Customer("0001", "DuongVanHuy", "Quen mat khau");
        Ticket t1 = new Ticket("0001-DVH", "Quen mat khau");
        customerQueue.add(t1);
        //Khach B den
        Customer b = new Customer("0002", "DuongThuyLinh", "Tao tai khoan moi");
        Ticket t2 = new Ticket("0002-DTL", "Tao tai khoan moi");
        customerQueue.add(t2);


        // Xu ly a
        Ticket h1 = customerQueue.handlingTicket();
        messageHistory.addMessage(new Message("0001-DVH", "Quy khach vui long nhap SDT da dang ky tai khoan"));
        messageHistory.addMessage(new Message("0001-DVH", "Quy khach vui long nhap ma xac thuc vua duoc gui den"));
        messageHistory.addMessage(new Message("0001-DVH", "Quy khach vui long cho trong giay lat"));

        messageHistory.undo();

        Ticket h2 = customerQueue.handlingTicket();
        messageHistory.addMessage(new Message("0002-DTL", "Quy khach vui long nhap so dien thoai dang ky"));

    }
}




class Message {
    private String id;
    private String content;

    public Message(String id, String content){
        this.id = id;
        this.content = content;
    }

    public String getContent(){
        return content;
    }

    public String getId(){
        return id;
    }
}

class Customer {
    private String id;
    private String name;
    private String issue;

    public Customer(String id, String name, String issue){
        this.id = id;
        this.name = name;
        this.issue = issue;
    }
}

class Ticket {
    private String id;
    private String content;
    private LocalDateTime timestamp;

    public Ticket(String id, String content){
        this.id = id;
        this.content = content;
        timestamp = LocalDateTime.now();
    }

    public void showInfo(){
        System.out.println("Ticket id: " + id);
        System.out.println("Content: " + content);
        System.out.println("Timestamp: " + timestamp);
    }
}


class MessageHistory {
    private Stack<Message> messageHistory = new Stack();

    public void addMessage(Message m){
        messageHistory.add(m);
    }

    public void undo(){
        if (messageHistory.isEmpty()){
            System.out.println("Khong con tin nhan cu hon");
        }
        messageHistory.pop();
        System.out.println("Da undo thanh cong");
    }

    public void viewLast(){
        if (messageHistory.isEmpty()){
            System.out.println("Khong con tin nhan cu hon de xem");
        }
        System.out.println("Last message: " + messageHistory.peek());
    }
}

class CustomerQueue {
    LinkedList<Ticket> customerQueue = new LinkedList<>();

    public void add(Ticket t){
        customerQueue.add(t);
    }
    public Ticket handlingTicket(){
        if (customerQueue.isEmpty()){
            System.out.println("Khong con khach hang can xu ly");
            return null;
        }
        Ticket t = customerQueue.poll();
        System.out.println("Da lay ticket");
        t.showInfo();
        return t;
    }
}