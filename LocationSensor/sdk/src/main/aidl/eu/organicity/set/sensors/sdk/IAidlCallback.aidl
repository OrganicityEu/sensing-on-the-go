// IAidlServiceCallback.aidl
package eu.organicity.set.sensors.sdk;

// Declare any non-default types here with import statements
import eu.organicity.set.sensors.sdk.JsonMessage;

interface IAidlCallback {
    void handlePluginInfo(in JsonMessage pluginInfo);
}