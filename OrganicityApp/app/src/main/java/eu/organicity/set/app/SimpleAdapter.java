package eu.organicity.set.app;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

/**
 * Created by chris on 19/10/2017.
 */

public class SimpleAdapter extends ArrayAdapter<Sensor> {

    private Context context;
    private ArrayList<Sensor> items;

    public SimpleAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Sensor[] objects) {
        super(context, resource, objects);
        this.context = context;
        this.items = new ArrayList<>(Arrays.asList(objects));
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.plugin_row, parent, false);

        TextView textView = (TextView) convertView.findViewById(R.id.pkg);
        textView.setText(items.get(position).getName());

        TextView textView1 = (TextView) convertView.findViewById(R.id.servicename);
        textView1.setText(items.get(position).getPkg());

        return convertView;
    }

    @Override
    public int getCount() {
        return items.size();
    }
}
