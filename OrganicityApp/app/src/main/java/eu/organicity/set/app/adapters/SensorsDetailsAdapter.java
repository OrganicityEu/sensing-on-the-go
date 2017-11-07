package eu.organicity.set.app.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import eu.organicity.set.app.R;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

/**
 * Created by chris on 23/10/2017.
 */

public class SensorsDetailsAdapter extends RecyclerView.Adapter<SensorsDetailsAdapter.ViewHolder> {

    private List<Sensor> items;

    public SensorsDetailsAdapter(List<Sensor> items) {
        super();
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_card_row, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Sensor sensor = items.get(position);

        holder.title.setText(sensor.getName().trim());
//        holder.description.setText(sensor.());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;


        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.name);
        }
    }
}
