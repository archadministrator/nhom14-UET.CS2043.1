//                 <<abstract>>
//                  Device
//         -------------------------
//         - id: String
//         - name: String
//         - isOn: boolean
//         -------------------------
//         + turnOn()
//         + turnOff()
//                 ▲
//                 |
//    -------------------------------------
//    |        |           |             |
//  Light   AirConditioner  Speaker    Curtain


// <<interface>>                <<interface>>
//  WifiConnectable             Adjustable
// ------------------         ------------------
// + connectWifi()            + increase()
//                            + decrease()


// Quan hệ:

// AirConditioner ─── WifiConnectable
// Speaker        ─── WifiConnectable

// Speaker        ─── Adjustable
// Light          ─── Adjustable


// Hub
// -------------------------
// - devices: List<Device>
// -------------------------
// + turnOffAll()
// + setupWifi()

// Hub ── Device (aggregation)

import java.util.*;



public class Bai8{
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();
        List<Device> Hub = new ArrayList<>();

        for (int i = 0; i < n; i++){
            String[] inputLine = scanner.nextLine().trim().split("\\s+");
            if (inputLine[0].equals("L")){
                Hub.add(new SmartLight(inputLine[1], inputLine[2], false));
            }
            else if (inputLine[0].equals("AC")){
                Hub.add(new AirConditioner(inputLine[1], inputLine[2], false));
            }
            else if (inputLine[0].equals("S")){
                Hub.add(new SmartSpeaker(inputLine[1], inputLine[2], false));
            }
            else if (inputLine[0].equals("C")){
                Hub.add(new Curtain(inputLine[1], inputLine[2], false));
            }
        }

        scanner.close();

        System.out.println("Turn Off All Devices: ");
        Hub.forEach(device -> device.turnOff());

        System.out.println();
        System.out.println("Setup Wifi: ");
        Hub.forEach(device -> {
            if (device instanceof WifiConnectable){
                ((WifiConnectable) device).connect();
            }
        });

    }
}


interface Toggleable {void turnOn(); void turnOff();}

interface Adjustable {void increase(); void decrease();}

interface WifiConnectable{void connect(); void disconnect();}

abstract class Device implements Toggleable{
    protected String id;
    protected String name;
    protected boolean isOn;

    public Device(String id, String name, boolean isOn){
        this.id = id;
        this.name = name;
        this.isOn = isOn;
    }

    @Override
    public void turnOn(){
        isOn = true;
        System.out.println(name + "turned on");
    }

    public void turnOff(){
        isOn = false;
        System.out.println(name + " turned off");
    }


}

class SmartLight extends Device implements  Adjustable{
    protected int brightness = 5;
    public SmartLight(String id, String name, boolean isOn){
        super(id, name, isOn);
    }


    // Adjustable Interface
    @Override
    public void increase(){
        if (brightness == 10) {
            System.out.println("Bightness has reached the maximum");
        } else {
            brightness += 1;
            System.out.println("Brightness has been increased to " + brightness);
        }
    }
    @Override
    public void decrease(){
        if (brightness == 1){
            System.out.println("Brightness has reached the minimum");
        } else {
            brightness -= 1;
            System.out.println("Brightness has been decreased to " + brightness);
        }
    }
}


class AirConditioner extends Device implements  WifiConnectable {
    protected boolean connected = false;
    public AirConditioner(String id, String name, boolean isOn){
        super(id, name, isOn);
    }       

    //WifiConnectable Interface
    @Override
    public void connect(){
        connected = true;
        System.out.println(name + " connected to wifi");
    }
    @Override 
    public void disconnect(){
        connected = false;
        System.out.println(name + " disconnected from wifi");
    }
}

class SmartSpeaker extends Device implements Adjustable, WifiConnectable {
    protected boolean connected = false;
    protected int volume = 5;

    public SmartSpeaker(String id, String name, boolean isOn){
        super(id, name, isOn);
    }

    // Adjustable Interface
    @Override 
    public void increase(){
        if (volume >= 10){
            System.out.println("Volume has reached the maxium");
        } else {
            volume += 1;
            System.out.println("Volume has been increased to " + volume);
        }
    }
    @Override 
    public void decrease(){
        if (volume <= 0){
            System.out.println("Volume has reached the minumum");
        } else {
            volume -= 1;
            System.out.println("Volume has been decreased to " + volume);
        }
    }

    //WifiConnectable Interface 
    @Override
    public void connect(){
        connected = true;
        System.out.println(name + " connected to wifi");
    }
    @Override 
    public void disconnect(){
        connected = false;
        System.out.println(name + " disconnected from wifi");
    }
}


class Curtain extends Device {
    public Curtain(String id, String name, boolean isOn){
        super(id, name, isOn);
    }
}