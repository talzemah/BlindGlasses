package talzemah.blindglasses;

import java.util.ArrayList;

public class ResultsFilter { // singleton

    private static final float QUALITY_THRESHOLD = 0.6f;
    private static final int MIN_THRESHOLD = 4;
    private static final int MAX_THRESHOLD = 6;
    private static final int MAX_COLOR_THRESHOLD = 3;


    private ArrayList<Result> filterResArr;
    private ArrayList<Result> colorResArr;


    public ResultsFilter() {

        filterResArr = new ArrayList<>();
        colorResArr = new ArrayList<>();
    }

    public ArrayList<Result> startFiltering(ArrayList<Result> resArr) {

        resetFilterResArr();

        filterQualityThreshold(resArr);
        filterMinThreshold(resArr);
        filterMaxThreshold();

        mergeResultsWithColor();

        return filterResArr;
    }


    private void resetFilterResArr() {
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

    private void mergeResultsWithColor() {
        for (int i = 0; i < colorResArr.size(); i++) {
            filterResArr.add(colorResArr.get(i));
        }
    }


    // Getter & Setter

    public ArrayList<Result> getFilterResArr() {
        return filterResArr;
    }

    public void setFilterResArr(ArrayList<Result> filterResArr) {
        this.filterResArr = null; // todo is necessary?
        this.filterResArr = filterResArr;
    }

} // End class
