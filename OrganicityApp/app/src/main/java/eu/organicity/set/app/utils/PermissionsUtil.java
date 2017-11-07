package eu.organicity.set.app.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by chris on 27/06/2017.
 */

public class PermissionsUtil {

    public static final int PERMISSIONS_REQUEST = 1002;

    public static boolean checkLocationPermissions(Activity activity) {
        return checkPermissions(activity, "android.permission.ACCESS_FINE_LOCATION")
                || checkPermissions(activity, "android.permission.ACCESS_COARSE_LOCATION");
    }

    public static boolean checkPermissions(Activity activity, String[] permissions) {
        boolean hasPermission = true;

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
                hasPermission = false;
            }
        }

        return hasPermission;
    }

    public static boolean checkPermissions(Activity activity, String permissions) {
        return ContextCompat.checkSelfPermission(activity, permissions) == PackageManager.PERMISSION_GRANTED;
    }

    public static void askAllPermissions(Activity activity) {

        String[] permissions = new String[] {
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                WRITE_EXTERNAL_STORAGE,
                READ_PHONE_STATE,
        };

        ActivityCompat.requestPermissions(activity,
                permissions,
                PERMISSIONS_REQUEST);

    }
}
