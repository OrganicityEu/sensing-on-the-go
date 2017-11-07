// IAidlService.aidl
package eu.organicity.set.app.sdk;

// Declare any non-default types here with import statements
import eu.organicity.set.app.sdk.ISensorCallback;

interface ISensorService {
    void getPluginInfo(in ISensorCallback callback);
}
