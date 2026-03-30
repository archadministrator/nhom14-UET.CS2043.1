import java.util.*;

public class Bai13 {
    public static void main(String[] args) {
        String s = "This is a sample text. This text is simple, and this is just a sample!";
        wordFrequencyCounter(handlingText(s));
    }

    
    public static String[] handlingText(String s){
        s = s.toLowerCase().replaceAll("[^a-z]", " ");
        String[] words = s.split("\\s+");
        return words;
    }
    public static void wordFrequencyCounter(String[] words){
        HashMap<String, Integer> wordCount = new HashMap<>();
        for (String word: words){
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        String mostFrequentWord = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry: wordCount.entrySet()){
            if (entry.getValue() > maxCount) {
                mostFrequentWord = entry.getKey();
                maxCount = entry.getValue();
            } 
        }

        List<String> uniqueWords = new ArrayList<>();

        for (Map.Entry<String, Integer> entry: wordCount.entrySet()){
            if (entry.getValue() == 1) {
                uniqueWords.add(entry.getKey());
            } 
        }

        System.out.println("Most frequency word: " + mostFrequentWord);
        System.out.println("Unique word(s): ");
        uniqueWords.forEach(word -> System.out.println(word));
    }
}