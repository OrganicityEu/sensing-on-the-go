package eu.smartsantander.androidExperimentation.jsonEntities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Entities {
    private Data data;

    public Entities() {
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Entities{" +
                "data=" + data +
                '}';
    }
}