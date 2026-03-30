
public class Bai11 {
    public static void main(String[] args) {
            useString();
            useStringBuffer();
    }


    public static void useString(){
        String s = "";

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++){
            s += "hello";
        }
        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }

    public static void useStringBuffer(){
        StringBuffer s = new StringBuffer();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++){
            s.append("Hello");
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }

    public static void contentAnalysis(String s){
        int sentences = 0;
        for (int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if (c == '.' || c == '?' || c == '!'){
                sentences ++;
            }
        }
        String result = s.replace("Java", "Python");
        System.out.println("Sentences: " + sentences);
        System.out.println(result);

    }
}
