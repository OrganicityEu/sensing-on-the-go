package eu.organicity.set.app;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import eu.smartsantander.androidExperimentation.util.Discoverable;

/**
 * Created by chris on 19/10/2017.
 */

public class SimpleAdapter extends ArrayAdapter<Discoverable> {

    private Context context;
    private ArrayList<Discoverable> items;

    private HashMap<Integer, Boolean> checked;

    public SimpleAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.plugin_row, parent, false);

        TextView textView = (TextView) convertView.findViewById(R.id.pkg);
        textView.setText(items.get(position).getName());

        TextView textView1 = (TextView) convertView.findViewById(R.id.servicename);
        textView1.setText(items.get(position).getPkg());

        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
        checkBox.setChecked(checked.get(position));

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                checked.put(position, b);
            }
        });

        return convertView;
    }

    @Override
    public int getCount() {
        if (items == null) {
            return 0;
        }

        return items.size();
    }

    public void setItems(ArrayList<Discoverable> items) {
        this.items = items;

        checked = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            checked.put(i, false);
        }
    }

    public ArrayList<Discoverable> getCheckedItems() {
        ArrayList<Discoverable> result = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            if (checked.get(i)) {
                result.add(items.get(i));
            }
        }

        return result;
    }
}
