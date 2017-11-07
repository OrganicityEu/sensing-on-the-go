package eu.organicity.set.app.sdk;

import com.google.gson.Gson;

public class Report {

    private String experimentId;
    private int  deviceId;
    private String jobResults;

    public Report()
    {
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public Report(String jobName)
    {
        this.experimentId = jobName;
    }

    public void setResults(String jobResults)
    {
        this.jobResults = jobResults;
    }

    public void setJobResults(String jobResults)
    {
        this.jobResults = jobResults;
    }

    public String getResults()
    {
        return this.jobResults;
    }

    public String getJobResults()
    {
        return this.jobResults;
    }

    public String getName()
    {
        return experimentId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String toJson(){
        return (new Gson()).toJson(this);
    }

    public static Report fromJson(String json){
        return (new Gson()).fromJson(json, Report.class);
    }
}
