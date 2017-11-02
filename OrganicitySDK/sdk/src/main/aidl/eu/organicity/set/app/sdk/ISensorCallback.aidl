// IAidlCallback.aidl
package eu.organicity.set.app.sdk;

// Declare any non-default types here with import statements
import eu.organicity.set.app.sdk.JsonMessage;

interface ISensorCallback {
    void handlePluginInfo(in JsonMessage pluginInfo);
}
