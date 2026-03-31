public class Bai4 {
    public static void main(String[] args) {
        DataManager dataManager = new DataManager();
        dataManager.show();
    }
}



interface IData {void show();}

class DataManager implements IData {
    // Error: show() in DataManager cannot implement show() in IData attempting to assign weaker of the inherited method from IData
    //Mặc định trong interface là public, nhưng mặc định trong class là default (package-private)
    public void show(){
        System.out.println("Show Data");
    }
}


