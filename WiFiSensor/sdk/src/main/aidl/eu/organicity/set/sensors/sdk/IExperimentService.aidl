// IExperimentService.aidl
package eu.organicity.set.sensors.sdk;

// Declare any non-default types here with import statements
import eu.organicity.set.sensors.sdk.JsonMessage;

interface IExperimentService {
    void getExperimentResult(in JsonMessage message, out JsonMessage results);
}
