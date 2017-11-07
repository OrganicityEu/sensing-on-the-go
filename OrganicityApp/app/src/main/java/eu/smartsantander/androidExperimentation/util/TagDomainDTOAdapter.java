package eu.smartsantander.androidExperimentation.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import eu.organicity.annotation.common.dto.TagDomainDTO;

public class TagDomainDTOAdapter extends ArrayAdapter<TagDomainDTO> {

    // Your sent context
    private Context context;
    // Your custom values for the spinner (User)
    private ArrayList<TagDomainDTO> values;

    public TagDomainDTOAdapter(Context context, int textViewResourceId, ArrayList<TagDomainDTO> values) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
    }

    public int getCount() {
        return values.size();
    }

    public TagDomainDTO getItem(int position) {
        return values.get(position);
    }

    public long getItemId(int position) {
        return position;
    }


    // And the "magic" goes here
    // This is for the "passive" state of the spinner
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        TagDomainDTO element = values.get(position);
        label.setText(element.getDescription());
        // And finally return your dynamic (or custom) view for each spinner item
        return label;
    }

    // And here is when the "chooser" is popped up
    // Normally is the same view, but you can customize it if you want
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        TagDomainDTO element = values.get(position);
        label.setText(element.getDescription());
        return label;
    }
}
