package eu.organicity.set.app.activities;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;

import eu.organicity.set.app.BuildConfig;
import eu.organicity.set.app.R;
import eu.organicity.set.app.utils.AccountUtils;
import eu.organicity.set.app.utils.Constants;
import eu.organicity.set.app.utils.PermissionsUtil;
import eu.smartsantander.androidExperimentation.util.GenericDialogListener;
import eu.smartsantander.androidExperimentation.util.OrganicityOAuthDialog;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class LoginActivity extends Activity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    private Context context;

    private View logo;
    private View form;
    private TextView developer;

    /**
     * Duration of wait
     **/
    private final int SPLASH_DISPLAY_LENGTH = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        form = findViewById(R.id.form);
        logo = findViewById(R.id.logo);

        context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionsUtil.askAllPermissions(this);
        } else {
            startMain();
        }

        final TextView logginButton = (TextView) findViewById(R.id.btn_login);
        logginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                final long nonce = new SecureRandom().nextLong();
                String authRequestRedirect = Constants.ORGANICITY_APP_OAUTH_BASEURL + Constants.ORGANICITY_APP_OAUTH_URL
                        + "?client_id=" + BuildConfig.OC_APP_ID
                        + "&redirect_uri=" + Constants.ORGANICITY_APP_CALLBACK_OAUTHCALLBACK
//                        + "&scope=user"
                        + "&response_type=code"
                        + "&state=" + System.currentTimeMillis()
                        + "&nonce=" + nonce
                        + "&scope=offline_access"
//                        + "&display=touch"
                        ;

                CookieSyncManager.createInstance(context);
                new OrganicityOAuthDialog(context, authRequestRedirect
                        , new GenericDialogListener() {
                    public void onComplete(final Bundle values) {
                        /* Create an Intent that will start the Menu-Activity. */
                        final Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        LoginActivity.this.startActivity(mainIntent);
                        LoginActivity.this.finish();
                    }


                    public void onError(String e) {
                    }

                    public void onCancel() {
                    }
                }).show();


            }
        });
        final TextView anonymousTextView = (TextView) findViewById(R.id.link_anonymous);
        anonymousTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anonymousTextView.setText("Starting up! please wait...");
                  /* Create an Intent that will start the Menu-Activity. */
                final Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                LoginActivity.this.startActivity(mainIntent);
                LoginActivity.this.finish();
            }
        });

        // For testing only
        anonymousTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                 /* Create an Intent that will start the Menu-Activity. */
                final Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
//                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                LoginActivity.this.startActivity(mainIntent);
                LoginActivity.this.finish();

                return true;
            }
        });

//        anonymousTextView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                 /* Create an Intent that will start the Menu-Activity. */
//                final Intent mainIntent = new Intent(LoginActivity.this, TestActivity.class);
////                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                LoginActivity.this.startActivity(mainIntent);
//                LoginActivity.this.finish();
//
//                return true;
//            }
//        });

        developer = (TextView) findViewById(R.id.developer);
        developer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent mainIntent = new Intent(LoginActivity.this, TestActivity.class);
                LoginActivity.this.startActivity(mainIntent);
                LoginActivity.this.finish();
            }
        });
    }

    private void startMain() {
        if (AccountUtils.getOfflineToken() != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    LoginActivity.this.startActivity(mainIntent);
                    LoginActivity.this.finish();
                }
            }, SPLASH_DISPLAY_LENGTH);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    logo.animate().translationY(-logo.getHeight() / 2).setDuration(SPLASH_DISPLAY_LENGTH / 3).start();
                    form.animate().alpha(1).setStartDelay(SPLASH_DISPLAY_LENGTH / 3).start();
                    developer.animate().alpha(1).setStartDelay(SPLASH_DISPLAY_LENGTH / 3).start();
                }
            }, SPLASH_DISPLAY_LENGTH);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = true;
        switch (requestCode) {
            case PermissionsUtil.PERMISSIONS_REQUEST:
                for (int grantResult : grantResults) {
                    if (grantResult != PERMISSION_GRANTED) {
                        granted = false;
                        break;
                    }
                }
        }
        if (granted) {
            startMain();
        }
        else {
            Toast.makeText(this, "Please accept persmissions to continue", Toast.LENGTH_LONG).show();
        }
    }
}