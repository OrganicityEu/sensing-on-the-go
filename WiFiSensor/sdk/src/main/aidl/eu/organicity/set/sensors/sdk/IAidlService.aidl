// IAidlService.aidl
package eu.organicity.set.sensors.sdk;

// Declare any non-default types here with import statements
import eu.organicity.set.sensors.sdk.IAidlCallback;

interface IAidlService {
    void getPluginInfo(in IAidlCallback callback);
}
