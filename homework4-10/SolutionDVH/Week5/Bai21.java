import java.util.*;

public class Bai21 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            int a = scanner.nextInt();
            int b = scanner.nextInt();

            System.out.println(a/b);
        } catch (InputMismatchException e) {
            System.out.println("Vui long nhap 2 so nguyen cach nhau boi dau cach.");
        } catch (ArithmeticException e){
            System.out.println("So chia b khong the la 0");
        } finally {
            System.out.println("Program finished.");
        }
    }
}