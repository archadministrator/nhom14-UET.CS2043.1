import java.io.*;
import java.util.*;


public class Bai22 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Type in source file path");
        String srcPath = scanner.nextLine();

        System.out.println("Type in destination file path");
        String desPath = scanner.nextLine();

        BufferedReader br = null;
        PrintWriter pw = null;
        String line;
        int lineCount = 0;

        try {
            br = new BufferedReader(new FileReader(srcPath));
            pw = new PrintWriter(new FileWriter(desPath));

            while ((line = br.readLine()) != null){
                pw.println(line);
                lineCount += 1;
            }            
        } catch (FileNotFoundException e){
            if (br==null){
                System.out.println("Source file not found.");
            } else if (pw == null){
                System.out.println("Cannot create destination file.");
            }
        } catch (IOException e) {
            System.out.println("I/O error.");
            e.printStackTrace();
        } finally {
            try {
                if (pw != null) pw.close();
                if (br != null) br.close();
                System.out.println("Read: " + lineCount + " line(s)");
            } catch (IOException e) {
                System.out.println("Error when closing files.");
            }
        }
    }
}
