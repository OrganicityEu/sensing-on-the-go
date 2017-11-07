package eu.smartsantander.androidExperimentation.util;

public class Sensor {
	
	private final String sensorType;
	
	public Sensor(String sensorType)
	{
		this.sensorType = sensorType;
	}

	public String getSensorType()
	{
		return this.sensorType;
	}
	
}
