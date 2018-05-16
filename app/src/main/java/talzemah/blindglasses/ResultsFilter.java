package talzemah.blindglasses;

import java.util.ArrayList;

public class ResultsFilter {
    //todo  singleton?

    // This score of results are considered reliable enough.
    private static final float QUALITY_THRESHOLD = 0.6f;
    // Determines the minimum number of results to be taken.
    private static final int MIN_THRESHOLD = 4;
    // Determines the maximum number of results to be taken.
    private static final int MAX_THRESHOLD = 6;
    // Determines the maximum color results to be taken.
    private static final int MAX_COLOR_THRESHOLD = 3;


    private ArrayList<Result> filterResArr;
    private ArrayList<Result> colorResArr;
    private ArrayList<Result> previousFilterResArr;
    private ArrayList<Result> previousColorResArr;

    ResultsFilter() {

        filterResArr = new ArrayList<>();
        colorResArr = new ArrayList<>();
        previousFilterResArr = new ArrayList<>();
        previousColorResArr = new ArrayList<>();
    }

    public ArrayList<Result> startFiltering(ArrayList<Result> resArr) {

        preparingArrays();
        filterQualityThreshold(resArr);
        filterMinThreshold(resArr);
        filterMaxThreshold();
        preventDuplicates();
        mergeResultsWithColor();

        return filterResArr;
    }

    private void preparingArrays() {

        filterResArr.clear();
        colorResArr.clear();
    }

    private void filterQualityThreshold(ArrayList<Result> resArr) {

        for (int i = 0; i < resArr.size(); i++) {
            if (resArr.get(i).score >= QUALITY_THRESHOLD) {

                if (resArr.get(i).name.contains("color")) {
                    colorResArr.add(resArr.get(i));
                } else {
                    filterResArr.add(resArr.get(i));
                }

            } else {
                // For reasons of efficiency
                break;
            }
        }
    }

    private void filterMinThreshold(ArrayList<Result> resArr) {

        for (int i = filterResArr.size() + colorResArr.size(); (filterResArr.size() < MIN_THRESHOLD) && resArr.size() > i; i++) {

            if (!resArr.get(i).name.contains("color")) {
                filterResArr.add(resArr.get(i));
            }
        }
    }

    private void filterMaxThreshold() {


        while (filterResArr.size() > MAX_THRESHOLD) {
            filterResArr.remove(filterResArr.size() - 1);
        }

        while (colorResArr.size() > MAX_COLOR_THRESHOLD) {
            colorResArr.remove(colorResArr.size() - 1);
        }

    }

    private void preventDuplicates() {
        ArrayList<Result> tempArr = new ArrayList<>();

        // Compare to previous time.
        if (!previousFilterResArr.isEmpty()) {

            for (Result res : filterResArr) {
                if (!previousFilterResArr.contains(res))
                    tempArr.add(res);
            }

            // Save all res for next time.
            previousFilterResArr = (ArrayList<Result>) filterResArr.clone();


            filterResArr = (ArrayList<Result>) tempArr.clone();
            tempArr.clear();

        } else {

            previousFilterResArr = (ArrayList<Result>) filterResArr.clone();
        }


        // Compare to previous time.
        if (!previousColorResArr.isEmpty()) {
            for (Result res : colorResArr) {
                if (!previousColorResArr.contains(res))
                    tempArr.add(res);
            }

            // Save all res for next time.
            previousColorResArr = (ArrayList<Result>) colorResArr.clone();

            colorResArr = (ArrayList<Result>) tempArr.clone();
            tempArr.clear();

        } else {

            previousColorResArr = (ArrayList<Result>) colorResArr.clone();
        }
    }

    private void mergeResultsWithColor() {
        filterResArr.addAll(colorResArr);
    }


    // Getter & Setter

    public ArrayList<Result> getFilterResArr() {
        return filterResArr;
    }

    public void setFilterResArr(ArrayList<Result> filterResArr) {
        this.filterResArr = filterResArr;
    }

} // End class
