package eu.organicity.set.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import eu.organicity.set.app.App;
import gr.cti.android.experimentation.client.OauthTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

import static eu.organicity.set.app.operations.Communication.OC_ACCOUNTS_PREF_NAME;
import static eu.organicity.set.app.operations.Communication.OC_REF_TOKEN_NAME;

public class AccountUtils {
    private static final String TAG = "AccountUtils";
    private static final String ACCOUNTS_TOKEN_ENDPOINT = "https://accounts.organicity.eu/realms/organicity/protocol/openid-connect/token";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static Jwt<Header, Claims> jwt;
    private static String accessToken;

    public static String getOfflineToken() {
        return getPreferences().getString(OC_REF_TOKEN_NAME, null);
    }

    public static String updateAccessToken() {

        final String offlineToken = getOfflineToken();
        if (offlineToken != null) {

            if (!isJwtExpired()) {
                Log.i(TAG, "Access token valid, reusing...");
                return accessToken;
            }
            Log.i(TAG, "Access token expired, requesting a new one...");

            final MultiValueMap<String, String> codeMap = new LinkedMultiValueMap<>();
            codeMap.add("grant_type", "refresh_token");
            codeMap.add("refresh_token", offlineToken);
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + "c21hcnRwaG9uZS1leHBlcmltZW50LW1hbmFnZW1lbnQ6YmI2ODFmZmItNDNiNi00OTRmLTgwYTItNDY3YjE1MDViNzkz");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            final HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(codeMap, headers);
            try {
                final ResponseEntity<OauthTokenResponse> response = restTemplate.exchange(ACCOUNTS_TOKEN_ENDPOINT, HttpMethod.POST, req, OauthTokenResponse.class);
                if (response.hasBody()) {
                    final OauthTokenResponse credentials = response.getBody();
                    accessToken = credentials.getAccess_token();
                    jwt = parseAccessToken(accessToken);
                    Log.i(TAG, "New access token acquired, stored in-memory copy!");
                } else {
                    Log.w(TAG, "Failed to acquire new access token, scrubbing in memory data!");
                    jwt = null;
                    accessToken = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to acquire new access token, scrubbing in memory data!", e);
                jwt = null;
                accessToken = null;
            }
        } else {
//            Log.w(TAG, "No offline token available!");
            accessToken = null;
            jwt = null;
        }

        return accessToken;
    }

    public static Jwt<Header, Claims> getJwt() {
        updateAccessToken();
        return jwt;
    }

    public static String getUserName() {
        if (jwt != null && jwt.getBody().containsKey("name")) {
            return (String) jwt.getBody().get("name");
        }
        return null;
    }

    /*
     * This reads the public key from the certificate
     */
    private static PublicKey getPublicKey() {
        try {

            final InputStream inputStream = App.getInstance().getApplicationContext().getAssets().open("key.pem");
            final CertificateFactory f = CertificateFactory.getInstance("X.509");
            final X509Certificate certificate = (X509Certificate) f.generateCertificate(inputStream);
            return certificate.getPublicKey();
        } catch (IOException | CertificateException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    public static void storeOfflineToken(final String refreshToken) {
        Log.i(TAG, "Storing new offline token...");
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(OC_REF_TOKEN_NAME, refreshToken);
        editor.apply();
    }

    public static void clearOfflineToken() {
        Log.i(TAG, "Scrubbing offline token...");
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.clear();
        editor.apply();
    }

    private static boolean isJwtExpired() {
        if (jwt!=null){
            Log.i(TAG,jwt.getBody().getExpiration().toString());
        };
        return jwt == null || !jwt.getBody().getExpiration().after(new Date());
    }

    private static Jwt<Header, Claims> parseAccessToken(final String accessToken) {
        return Jwts.parser().setSigningKey(getPublicKey()).parse(accessToken);
    }

    private static SharedPreferences getPreferences() {
        return App.getInstance().getApplicationContext().getSharedPreferences(OC_ACCOUNTS_PREF_NAME, Context.MODE_PRIVATE);
    }

}
