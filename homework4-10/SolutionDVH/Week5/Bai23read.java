import java.io.*;
import java.util.Scanner;

public class Bai23read {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Type in file path: ");
        String path = scanner.nextLine();

        DataInputStream dis = null;

        try {
            dis = new DataInputStream(new FileInputStream(path));

            while (true) {
                System.out.print(dis.readInt() + " ");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find file to read");
        } catch (EOFException e) {
            System.out.println("End of file reached.");
        } catch (IOException e) {
            System.out.println("I/O Error.");
            e.printStackTrace();
        } finally {
            try {
                if (dis != null) dis.close();
            } catch (IOException e) {
                System.out.println("Error when closing file.");
            }
        }
    }
}
