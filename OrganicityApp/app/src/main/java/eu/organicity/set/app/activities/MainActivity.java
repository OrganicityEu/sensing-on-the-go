package eu.organicity.set.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import eu.organicity.set.app.R;
import eu.organicity.set.app.fragments.ExperimentsFragment;
import eu.organicity.set.app.fragments.HomeFragment;
import eu.organicity.set.app.fragments.InfoFragment;
import eu.organicity.set.app.fragments.SensorsFragment;
import eu.organicity.set.app.operations.Communication;
import eu.organicity.set.app.services.SchedulerService;
import eu.organicity.set.app.utils.AccountUtils;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private int selectedItemId;
    private Toolbar toolbar;
    private TextView usernameTextView;
    private String username;

    BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishAffinity();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.home);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();

        Intent intent = new Intent(this, SchedulerService.class);
        startService(intent);

        IntentFilter intentFilter = new IntentFilter("kill-organicity");
        LocalBroadcastManager.getInstance(this).registerReceiver(killReceiver, intentFilter);

        final View header = navigationView.getHeaderView(0);

        final TextView versionTextView = (TextView) header.findViewById(R.id.version);
        try {
            final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionTextView.setText(getString(R.string.app_version, info.versionName, info.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e.getLocalizedMessage(), e);
        }

        final String offlineToken = AccountUtils.getOfflineToken();

        if (offlineToken != null) {
            Log.i(TAG, "offlineToken != null");
            Menu menu = navigationView.getMenu();
            MenuItem item = menu.findItem(R.id.login);
            item.setTitle("Logout");

            usernameTextView = (TextView) header.findViewById(R.id.name);
            Log.d(TAG, "getProfile");

            final SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            username = sharedPref.getString(getString(R.string.userName), null);
            if (username != null) {
                usernameTextView.setText(username);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String newUsername = AccountUtils.getUserName();
                        if (newUsername!=null && !newUsername.equals(username)) {
                            Log.d(TAG, "username: " + username);
                            username = newUsername;
                            usernameTextView.setText(username);
                            sharedPref.edit().putString(getString(R.string.userName), username).apply();

                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getLocalizedMessage(), e);
                    }
                }
            }).start();
        } else {
            Log.d(TAG, "offlineToken == null");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (usernameTextView != null && username != null) {
            usernameTextView.setText(username);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(killReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //        getMenuInflater().inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        selectDrawerItem(item);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void selectDrawerItem(MenuItem menuItem) {
        if (menuItem.getItemId() == selectedItemId) {
            return;
        }

        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass;
        String tag = null;

        switch (menuItem.getItemId()) {
            case R.id.home:
                fragmentClass = HomeFragment.class;
                tag = "HomeFragment";
                break;
            case R.id.sensors:
                fragmentClass = SensorsFragment.class;
                tag = "SensorsFragment";
                break;
            case R.id.experiments:
                fragmentClass = ExperimentsFragment.class;
                tag = "ExperimentFragment";
                break;
            case R.id.about:
                fragmentClass = InfoFragment.class;
                tag = "InfoFragment";
                break;
            //            case R.id.settings:
            //                mDrawer.closeDrawers();
            //
            //                startActivity(new Intent(this, DynamixPreferenceActivity.class));
            //                return;
            case R.id.login:
                if (menuItem.getTitle().equals("Logout")) {
                    logout();
                }

                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                MainActivity.this.finish();
                return;
            default:
                fragmentClass = HomeFragment.class;
                tag = "HomeFragment";
        }

        fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        toolbar.setTitle(menuItem.getTitle());

        selectedItemId = menuItem.getItemId();
    }

    private void logout() {
        try {
            AccountUtils.clearOfflineToken();
            final SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            sharedPref.edit().remove(getString(R.string.userName)).apply();
            new Communication().disconnectUser();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
