
public class Bai4a {
    public static void main(String[] args) {
        SorterAdapter sorterAdapter = new SorterAdapter(new LegacySorter());
        int[] arr = {5,3,6,1,13};
        arr = sorterAdapter.sort(arr);
        for (int num: arr){
            System.out.print(num + " ");
        }
    }
}

interface Sorter {
    int[] sort(int[] arr);
}

class LegacySorter {

    public int[] quickSort(int[] arr) {
        quickSortHelper(arr, 0, arr.length - 1);
        return arr;
    }

    private void quickSortHelper(int[] arr, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(arr, low, high);

            quickSortHelper(arr, low, pivotIndex - 1);
            quickSortHelper(arr, pivotIndex + 1, high);
        }
    }

    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high]; 
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}

class SorterAdapter implements Sorter {
    private LegacySorter legacySorter;
    public SorterAdapter(LegacySorter legacySorter){
        this.legacySorter = legacySorter;
    }
    //Core
    @Override 
    public int[] sort(int[] arr){
        return legacySorter.quickSort(arr);
    }
}



