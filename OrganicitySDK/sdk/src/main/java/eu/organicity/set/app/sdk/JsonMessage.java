package eu.organicity.set.app.sdk;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by chris on 18/10/2017.
 */

public class JsonMessage implements Parcelable {

    JSONObject jsonObject;
    String payload = "";
    String state;

    public JsonMessage() {
        jsonObject = new JSONObject();
    }

    public void put(String key, String value) {
        try {
            jsonObject.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void put(String key, JSONArray jsonArray) {
        try {
            jsonObject.put(key, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void put(String key, JSONObject object) {
        try {
            jsonObject.put(key, object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setPayload(List<Reading> r) {
        this.payload = (new Gson()).toJson(r);
    }

    public String getPayload() {
        return this.payload;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state=state;

    }

    protected JsonMessage(Parcel in) {
//        try {
//            jsonObject = new JSONObject(in.readString());
//        } catch (JSONException e) {
//            e.printStackTrace();

            this.payload = in.readString();
            this.state = in.readString();
//        }
    }

    public static final Creator<JsonMessage> CREATOR = new Creator<JsonMessage>() {
        @Override
        public JsonMessage createFromParcel(Parcel in) {
            return new JsonMessage(in);
        }

        @Override
        public JsonMessage[] newArray(int size) {
            return new JsonMessage[size];
        }
    };

    public JSONObject getJSON() {
        return jsonObject;
    }

    public void setJSON(JSONObject json) {
        jsonObject = json;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
//        if (jsonObject == null) {
            parcel.writeString(this.payload);
            parcel.writeString(this.state);
//        }
//        else {
//            parcel.writeString(jsonObject.toString());
//        }
    }

    public void readFromParcel(Parcel in) {
//        String readString = in.readString();

//        try {
//            this.jsonObject = new JSONObject(readString);
//        } catch (JSONException e) {
//            e.printStackTrace();

            this.payload = in.readString();
            this.state = in.readString();
//        }
    }
}
