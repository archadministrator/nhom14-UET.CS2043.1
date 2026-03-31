import java.io.*;
import java.util.*;

public class Bai23write {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Type in file name /file path");
        String path = scanner.nextLine();

        System.out.println("Type in n: ");
        int n = scanner.nextInt();

        DataOutputStream dos = null;

        try {
            dos = new DataOutputStream(new FileOutputStream(path));
            
            for (int i = 0; i < n; i++){
                int num = scanner.nextInt();
                dos.write(num);
            }
        } catch (FileNotFoundException e){
            System.out.println("File path can't reach");
        } catch (IOException e){
            System.out.println("I/O error.");
            e.printStackTrace();
        }


    }
}