class BankAccount {
    int balance = 0;

    public synchronized void deposit(int amount) {
        balance += amount;
    }

    public synchronized void withdraw(int amount) {
        balance -= amount;
    }
}

public class Bai3 {
    public static void main(String[] args) throws Exception {
        BankAccount acc = new BankAccount();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) acc.deposit(100);
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) acc.withdraw(100);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println(acc.balance);
    }
}