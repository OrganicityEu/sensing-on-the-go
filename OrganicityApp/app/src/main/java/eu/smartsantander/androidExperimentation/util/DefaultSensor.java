package eu.smartsantander.androidExperimentation.util;

import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Created by amaxilatis on 8/12/2015.
 */
public class DefaultSensor {
    private final String name;
    private final String description;
    private final boolean state;
    private final CompoundButton.OnCheckedChangeListener listener;

    public DefaultSensor(final String name, final String description, final boolean state, final Switch.OnCheckedChangeListener listener) {
        this.name = name;
        this.description = description;
        this.state = state;
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isState() {
        return state;
    }

    public CompoundButton.OnCheckedChangeListener getListener() {
        return listener;
    }
}