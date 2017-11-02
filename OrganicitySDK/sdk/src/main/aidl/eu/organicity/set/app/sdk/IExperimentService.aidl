// IExperimentService.aidl
package eu.organicity.set.app.sdk;

// Declare any non-default types here with import statements
import eu.organicity.set.app.sdk.JsonMessage;

interface IExperimentService {
    void getExperimentResult(in Bundle message, out JsonMessage results);
}
