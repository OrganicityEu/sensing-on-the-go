package eu.smartsantander.androidExperimentation.util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import eu.organicity.set.app.R;
import eu.organicity.set.app.operations.Communication;


public class OrganicityOAuthDialog extends Dialog {

    private static final String TAG = "OrganicityOAuthDialog";

    /* Strings used in the OAuth flow */
    public static final String OAUTHCALLBACK_URI = Constants.ORGANICITY_APP_CALLBACK_OAUTHCALLBACK;

    static final int BG_COLOR = Color.argb(1, 239, 64, 112);
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private String mUrl;
    private GenericDialogListener mListener;
    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;
    private TextView mTitle;

    public OrganicityOAuthDialog(Context context, String url, GenericDialogListener listener) {
        super(context);
        mUrl = url;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(getContext());
        mContent.setOrientation(LinearLayout.VERTICAL);
        setUpTitle();
        setUpWebView();
        //better screen sizing
        addContentView(mContent, new FrameLayout.LayoutParams(
                WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN));
    }

    private void setUpTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable icon = getContext().getResources().getDrawable(
                R.drawable.about_icon);
        mTitle = new TextView(getContext());
        mTitle.setText(R.string.organicity_accounts);
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(BG_COLOR);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        mTitle.setCompoundDrawablesWithIntrinsicBounds(
                icon, null, null, null);
        mContent.addView(mTitle);
    }

    private void setUpWebView() {
        mWebView = new WebView(getContext());
        //clear contents to enable login with different accounts
        mWebView.clearCache(true);
        mWebView.clearHistory();
        mWebView.clearFormData();
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new OAuthWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );
        mContent.addView(mWebView);
    }

    private class OAuthWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String url) {
            if (url.startsWith(OAUTHCALLBACK_URI)) {
                mSpinner.show();
                Log.i(TAG, "url:" + url);
                Uri uri = Uri.parse(url);
                final String code = uri.getQueryParameter("code");
                Log.i(TAG, "code:" + code);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Communication com = new Communication();
                        boolean token = com.getToken(code, OAUTHCALLBACK_URI, getContext());

                        Bundle values = parseUrl(url);


                        String error = values.containsKey("error") ? values.getString("error") : null;
                        if (error == null) {
                            error = values.containsKey("error_type") ? values.getString("error_type") : null;
                        }

                        if (error == null) {
                            mListener.onComplete(values);
                        } else if (error.equals("access_denied") ||
                                error.equals("OAuthAccessDeniedException")) {
                            mListener.onCancel();
                        }

                        OrganicityOAuthDialog.this.dismiss();
                    }
                }).start();
                view.stopLoading();
                return false;
            } else {
                view.loadUrl(url);
                return true;
            }

        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            // TODO: pass error back to listener!
            OrganicityOAuthDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }

            if (!url.startsWith(OAUTHCALLBACK_URI)) {
                try {// to avoid crashing the app add try-catch block, avoid this stupid crash!
                    if (mSpinner != null && mSpinner.isShowing())// by YG
                        mSpinner.dismiss();
                } catch (Exception ex) {
                    Log.w(TAG, "wtf exception onPageFinished! " + ex.toString());
                }
            }
        }

    }

    /**
     * Parse a URL query and fragment parameters into a key-value bundle.
     *
     * @param url the URL to parse
     * @return a dictionary bundle of keys and values
     */
    public static Bundle parseUrl(String url) {
        // hack to prevent MalformedURLException
        try {
            URL u = new URL(url);
            Bundle b = decodeUrl(u.getQuery());
            b.putAll(decodeUrl(u.getRef()));
            return b;
        } catch (MalformedURLException e) {
            return new Bundle();
        }
    }

    public static Bundle decodeUrl(String s) {
        Bundle params = new Bundle();
        if (s != null) {
            String array[] = s.split("&");
            for (String parameter : array) {
                String v[] = parameter.split("=");
                // YG: in case param has no value
                if (v.length == 2) {
                    params.putString(URLDecoder.decode(v[0]),
                            URLDecoder.decode(v[1]));
                } else {
                    params.putString(URLDecoder.decode(v[0]), " ");
                }
            }
        }
        return params;
    }

}


