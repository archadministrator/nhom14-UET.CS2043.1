import java.util.*;

public class Bai14 {
    public static void main(String[] args) {
        String text = "Hello world. This is a java program. Hello java, hello world.";

        WordCounter.displayResult(WordCounter.analyze(text));

    }
}

class WordCounter {
    public static HashMap<String, Integer> analyze(String text){
        text = text.toLowerCase().replaceAll("[^a-z ]", "");
        String[] words = text.split("\\s+");

        HashMap<String, Integer> wordCount = new HashMap<>();
        for (String word: words){
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        return wordCount;
    }

    public static void displayResult(HashMap<String, Integer> wordCount){
        for(Map.Entry<String, Integer> entry: wordCount.entrySet()){
            System.out.println("Word: " + entry.getKey() + " - " + "Frequency: " + entry.getValue() );
        }

        String mostFrequentWord = "";
        int maxCount = 0;

        for(Map.Entry<String, Integer> entry: wordCount.entrySet()){
            if (entry.getValue() > maxCount){
                mostFrequentWord = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        System.out.println("Most frequent word: " + mostFrequentWord);
    }
}

