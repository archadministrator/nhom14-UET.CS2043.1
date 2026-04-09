import java.util.*;
import java.util.concurrent.locks.*;

class BookStore {
    Map<String, Integer> stock = new HashMap<>();
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public int getStock(String title) {
        lock.readLock().lock();
        try {
            return stock.getOrDefault(title, 0);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addBook(String title, int qty) {
        lock.writeLock().lock();
        try {
            stock.put(title, stock.getOrDefault(title, 0) + qty);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void borrow(String title, int qty) {
        lock.writeLock().lock();
        try {
            stock.put(title, stock.getOrDefault(title, 0) - qty);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

public class Bai4 {
    public static void main(String[] args) {
        BookStore store = new BookStore();
        store.addBook("A", 10);
        store.addBook("B", 5);

        Runnable reader = () -> {
            System.out.println(Thread.currentThread().getName() + " read A=" + store.getStock("A"));
        };

        Runnable writer1 = () -> store.borrow("A", 2);
        Runnable writer2 = () -> store.addBook("A", 3);

        new Thread(reader).start();
        new Thread(reader).start();
        new Thread(reader).start();
        new Thread(writer1).start();
        new Thread(writer2).start();
    }
}