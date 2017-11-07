package eu.smartsantander.androidExperimentation.util;

import android.util.Log;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import eu.smartsantander.androidExperimentation.exception.UnauthorizedException;

public class MyResponseErrorHandler implements ResponseErrorHandler {

    private static final String TAG = "ErrorHandler";

    @Override
    public void handleError(ClientHttpResponse clienthttpresponse) throws IOException {
        if (clienthttpresponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
            Log.e(TAG, "Text:" + clienthttpresponse.getStatusText());
            BufferedReader reader = new BufferedReader(new InputStreamReader(clienthttpresponse.getBody()));
            StringBuilder sb = new StringBuilder();
            while (reader.readLine() != null) {
                sb.append(reader.readLine());
            }
            reader.close();
            Log.e(TAG, "result:" + sb.toString());
        } else if (clienthttpresponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            Log.e(TAG, "Text:" + clienthttpresponse.getStatusText());
            throw new UnauthorizedException(HttpStatus.UNAUTHORIZED, clienthttpresponse.getStatusText());
        } else {
            Log.e(TAG, "Text:" + clienthttpresponse.getStatusText());
            BufferedReader reader = new BufferedReader(new InputStreamReader(clienthttpresponse.getBody()));
            StringBuilder sb = new StringBuilder();
            while (reader.readLine() != null) {
                sb.append(reader.readLine());
            }
            reader.close();
            Log.e(TAG, "result:" + sb.toString());
        }
    }

    @Override
    public boolean hasError(ClientHttpResponse clienthttpresponse) throws IOException {
        if (clienthttpresponse.getStatusCode() != HttpStatus.OK) {
            return true;
        }
        return false;
    }
}