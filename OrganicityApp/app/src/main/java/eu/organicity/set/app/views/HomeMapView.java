package eu.organicity.set.app.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

/**
 * Created by chris on 16/07/2017.
 */

public class HomeMapView extends MapView {

    public HomeMapView(Context context) {
        super(context);
    }

    public HomeMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public HomeMapView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public HomeMapView(Context context, GoogleMapOptions googleMapOptions) {
        super(context, googleMapOptions);
    }

    @Override
    public void getMapAsync(OnMapReadyCallback onMapReadyCallback) {
        super.getMapAsync(onMapReadyCallback);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Disallow ScrollView to intercept touch events.
                this.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
                // Allow ScrollView to intercept touch events.
                this.getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        // Handle MapView's touch events.
        super.dispatchTouchEvent(ev);
        return true;
    }
}
