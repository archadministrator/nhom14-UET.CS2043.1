public class Bai5 {
    public static void main(String[] args) {
        // Pair<String, Integer> pair1 = new Pair<>("Age", "20");
        Pair<String, Integer> pair1 = new Pair<>("Age", 20);

        Pair<String, String> pair2 = new Pair<>("Student ID", "SV001");
        Pair<Integer, Double> pair3 = new Pair<>(105, 21.5);
        System.out.println(pair1.toString());
        System.out.println(pair2.toString());
        System.out.println(pair3.toString());
    }
}


class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value){
        this.key = key;
        this.value = value;
    }

    public K getKey(){return key;}
    public V getValue(){return value;}

    public void setKey(K key){this.key = key;}
    public void setValue(V value){this.value = value;}

    @Override
    public String toString(){
        return key + " - " + value;
    }
}
