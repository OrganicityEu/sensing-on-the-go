package eu.smartsantander.androidExperimentation.jsonEntities;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.smartsantander.androidExperimentation.util.Discoverable;

/**
 * Created with IntelliJ IDEA.
 * User: theodori
 * Date: 9/4/13
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class Experiment implements Serializable, Discoverable {

    public enum State {
        RUNNING,
        STOPPED
    }

    private String id;
    private String description;
    private String urlDescription;
    private long timestamp;
    private String parentExperimentId;
    private boolean installed;

    private String sensorDependencies;
    private String name;
    private String userId;
    private String url;
    private String status;

    private String pkg;
    private String service;
    private String key;
    private State state;
    private List<Sensor> sensors;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSensorDependencies() {
        return sensorDependencies;
    }

    public void setSensorDependencies(String sensorDependencies) {
        this.sensorDependencies = sensorDependencies;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlDescription() {
        return urlDescription;
    }

    public void setUrlDescription(String urlDescription) {
        this.urlDescription = urlDescription;
    }

    public String getParentExperimentId() {
        return parentExperimentId;
    }

    public void setParentExperimentId(String parentExperimentId) {
        this.parentExperimentId = parentExperimentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public void createKey() {
        setKey(getPkg() + "." + getService());
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getType() {
        return "experiment";
    }

    public void setKey(String key) {
        this.key = key;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Experiment that = (Experiment) o;

        if (id != that.id) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (sensorDependencies != null ? !sensorDependencies.equals(that.sensorDependencies) : that.sensorDependencies != null)
            return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return !(userId != null ? !userId.equals(that.userId) : that.userId != null);

    }

    @Override
    public int hashCode() {
        int result = 31 * (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (sensorDependencies != null ? sensorDependencies.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    public static Experiment parseFromJSON(JSONObject jExp) throws JSONException {
        Experiment exp = new Experiment();

        if (jExp.has("title")) exp.setName(jExp.getString("title"));
        if (jExp.has("package")) exp.setPkg(jExp.getString("package"));
        if (jExp.has("service")) exp.setService(jExp.getString("service"));

        exp.setKey(exp.getPkg() + "." + exp.getService());

        if (jExp.has("sensors")) {
            List<Sensor> sensors = new ArrayList<>();

            JSONArray sArray = jExp.getJSONArray("sensors");
            for (int i = 0; i < sArray.length(); i++) {
                JSONObject s = sArray.getJSONObject(i);

                Sensor sensor = new Sensor(s.getString("title"), s.getString("package"), s.getString("service"));
                sensors.add(sensor);
            }

            exp.setSensors(sensors);
        }

        return exp;
    }
}
