public class Bai6 {
    public static void main(String[] args) {
        Integer[] integers = {5,1,3,2};
        String[] strings = {"Java", "C++", "Python"};
        Student[] students = {};

        ArrayUtils.sort(integers);
        ArrayUtils.sort(strings);
        //ArrayUtils.sort(students);

        for(Integer x: integers){
            System.out.print(x + " ");
        }
        System.out.println();

        for(String s: strings){
            System.out.print(s + " ");
        }

    }
}


class ArrayUtils {
    public static <T> void swap(T[] array, int i, int j){
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static <T extends Comparable<T>> void sort(T[] array){
        for(int i = 0; i < array.length-1; i++){
            for (int j = i +1; j < array.length; j++) {
                if(array[i].compareTo(array[j])>0){
                    swap(array, i, j);
                }
            }
        }
    }

}

class Student {}
