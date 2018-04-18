package talzemah.blindglasses;

/**
 * Created by Tal on 18/04/2018.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class CustomArrayAdapter extends ArrayAdapter<String> {

    private ArrayList<Result> resArr;
    private ArrayList<Result> filterResArr;

    public CustomArrayAdapter(Context context, int textViewResourceId, ArrayList<Result> resArr, ArrayList<Result> filterResArr) {

        super(context, textViewResourceId, convertToStringArray(resArr));
        this.resArr = resArr;
        this.filterResArr = filterResArr;
    }

    private static String[] convertToStringArray(ArrayList<Result> resArr) {
        String[] arr = new String[resArr.size()];


        for (int i = 0; i < arr.length; i++) {
            arr[i] = resArr.get(i).getname() + " " + resArr.get(i).getscore();
        }

        return arr;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //return super.getView(position, convertView, parent);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.custom_row_listview, parent, false);

        TextView label = (TextView) row.findViewById(R.id.rowTextView);
        ImageView icon = (ImageView) row.findViewById(R.id.icon);

        label.setText(resArr.get(position).getname() + " " + resArr.get(position).getscore());


        if (filterResArr.contains(resArr.get(position))) {
            icon.setImageResource(R.drawable.v);
        } else {
            icon.setImageResource(R.drawable.x);
        }

        return row;
    }
}
