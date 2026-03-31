import java.io.*;
import java.util.*;

public class Bai25 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type in file path: ");
        String path = scanner.nextLine();

        Map<String, String> data = new HashMap<>();
        String line;

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(path));
            while ((line=br.readLine())!=null){
                if (line.trim().isEmpty()) continue;

                String[] input = line.split("=");

                if (input.length != 2) {
                    throw new InvalidConfigException("Invalid format input: " + line);
                } else {
                data.put(input[0].trim(), input[1].trim());
                }
            }

            if (!data.containsKey("username") || !data.containsKey("timeout")){
                throw new InvalidConfigException("Username or Timeout parameter is missing");
            } 

            int timeoutInt = Integer.parseInt(data.get("timeout"));
            if (timeoutInt <=0) {
                throw new InvalidConfigException("Invalid timeout value");
            }

            if (data.containsKey("maxConnections")){
                int maxConnectionsInt = Integer.parseInt(data.get("maxConnections"));
                if (maxConnectionsInt < 1){
                    throw new InvalidConfigException("Invalid max connections value");
                }
            }

            System.out.println("Config loaded successfully.");
            for (Map.Entry<String, String> entry: data.entrySet()){
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

            


        } catch (FileNotFoundException e){
            System.out.println("Config file not found.");
        } catch (NumberFormatException e){
            System.out.println("Invalid number format.");
        } catch (InvalidConfigException e){
            System.out.println("Invalid config: " + e.getMessage());
        } catch (IOException e){
            System.out.println("I/O Error:");
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e){
                System.out.println("Error when closing file");
            }
            System.out.println("Program finished.");
            scanner.close();
        }
    }


}


class InvalidConfigException extends Exception {
    public InvalidConfigException(String message){
        super(message);
    }
}