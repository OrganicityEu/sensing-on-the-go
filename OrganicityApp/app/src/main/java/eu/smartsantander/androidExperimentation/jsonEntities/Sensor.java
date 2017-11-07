package eu.smartsantander.androidExperimentation.jsonEntities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by chris on 19/10/2017.
 */

public class Sensor implements Serializable {

    private String name;
    private String pkg;
    private String service;
    private String key;

    public Sensor(String name, String pkg, String service) {
        this.name = name;
        this.pkg = pkg;
        this.service = service;

        this.key = this.pkg + "." + this.service;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public static Sensor parseFromJSON(JSONObject jSen) throws JSONException {
        String name = jSen.getString("title");
        String pkg = jSen.getString("package");
        String service = jSen.getString("service");

        String key = pkg + "." + service;
        Sensor sensor = new Sensor(name, pkg, service);
        sensor.setKey(key);

        return sensor;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
