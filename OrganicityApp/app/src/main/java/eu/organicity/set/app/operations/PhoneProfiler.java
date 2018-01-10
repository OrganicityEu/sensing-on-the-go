package eu.organicity.set.app.operations;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import eu.organicity.set.app.AppModel;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.util.Constants;

public class PhoneProfiler extends Thread implements Runnable {
    private SharedPreferences pref;
    private Editor editor;
    private Boolean started = false;
    private int PHONE_ID = Constants.PHONE_ID_UNITIALIZED;
    private List<Experiment> experiments = new ArrayList<>();
    private long totalReadingsProducedCounter;

    private final String TAG = this.getClass().getSimpleName();

    private boolean isInitialised = false;
    private int deviceIdHash = -1;


    public PhoneProfiler() {
        this.PHONE_ID = Constants.PHONE_ID_UNITIALIZED;
    }


    public Boolean getStarted() {
        return started;
    }

    public void run() {
        startJob();
        started = true;
    }

    public void startJob() {

        try {
            Thread.sleep(2000);
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
            editor = pref.edit();
            if ((pref.contains("phoneId"))) {
                this.PHONE_ID = pref.getInt("phoneId", 0);
                if (this.PHONE_ID < 1) {
                    setPhoneId(Constants.PHONE_ID_UNITIALIZED);
                }
            } else {
                setPhoneId(Constants.PHONE_ID_UNITIALIZED);
            }

            if (pref.contains("totalReadingsProducedCounter")) {
                this.totalReadingsProducedCounter = pref.getLong("totalReadingsProducedCounter", 0);
                if (this.totalReadingsProducedCounter < 0)
                    this.totalReadingsProducedCounter = 0;
            } else {
                this.totalReadingsProducedCounter = 0;
            }
            editor.apply();
            isInitialised = true;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        String tmDevice = "866021024988643";
        String tmSerial = "89300100110408729303";
        String androidId = "4f7a558a8345ec73";

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        System.out.println(deviceId.hashCode());
    }

    public void register() {

        if (!isInitialised) {
            startJob();
        }

        if (AppModel.instance.isDeviceRegistered()) {
            AppModel.instance.mixpanelIdentify();
            return;
        }
        int phoneId = Constants.PHONE_ID_UNITIALIZED;
        final int[] serverPhoneId = new int[1];

        try {
            final TelephonyManager tm = (TelephonyManager) AppModel.instance.context.getSystemService(Context.TELEPHONY_SERVICE);

            final String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" + android.provider.Settings.Secure.getString(AppModel.instance.context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
                    String deviceId = deviceUuid.toString();

                    AppModel.instance.phoneProfiler.setDeviceIdHash(deviceId.hashCode());

                    try {
                        Log.i(TAG, "deviceId:" + deviceId.hashCode());
                        serverPhoneId[0] = Communication.getInstance().registerSmartphone(deviceId.hashCode(), getSensorRules());
                        Log.i(TAG, "Phone:" + serverPhoneId[0]);
                        if (serverPhoneId[0] <= 0) {
                            serverPhoneId[0] = Constants.PHONE_ID_UNITIALIZED;
                        } else {
                            AppModel.instance.phoneProfiler.setPhoneId(serverPhoneId[0]);
                            setLastOnlineLogin();

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        AppModel.instance.phoneProfiler.setPhoneId(Constants.PHONE_ID_UNITIALIZED);
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            AppModel.instance.phoneProfiler.setPhoneId(Constants.PHONE_ID_UNITIALIZED);
        }
    }


    public int getDeviceIdHash() {
        pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
        editor = pref.edit();
        deviceIdHash = pref.getInt("deviceIdHash", -1);

        if (deviceIdHash == -1) {
            final TelephonyManager tm = (TelephonyManager) AppModel.instance.context.getSystemService(Context.TELEPHONY_SERVICE);

            final String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" + android.provider.Settings.Secure.getString(AppModel.instance.context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

            final UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
            final String deviceId = deviceUuid.toString();
            deviceIdHash = deviceId.hashCode();
            setDeviceIdHash(deviceIdHash);
            return deviceIdHash;
        }
        return deviceIdHash;
    }

    public void setDeviceIdHash(int hash) {
        this.deviceIdHash = hash;
        if (editor == null || pref == null) {
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
            editor = pref.edit();
        }
        editor.putInt("deviceIdHash", this.deviceIdHash);
        editor.apply();
    }

    public int getPhoneId() {
        return this.PHONE_ID;
    }

    public void setPhoneId(int PHONE_ID) {
        this.PHONE_ID = PHONE_ID;
        if (editor == null || pref == null) {
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
            editor = pref.edit();
        }
        editor.putInt("phoneId", this.PHONE_ID);
        editor.putString("experiment", "");
        editor.putLong("lastOnlineLoginDate", (new Date()).getTime());
        editor.putLong("totalReadingsProducedCounter", 0);
        editor.apply();

        AppModel.instance.mixpanelIdentify();
    }

    public void savePrefs() {
        if (editor == null || pref == null) {
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
            editor = pref.edit();
        }
        //editor.putInt("phoneId", this.PHONE_ID);
        String experimentsJson = (new Gson()).toJson(experiments);
        editor.putString("experiments", experimentsJson);
        editor.putLong("lastOnlineLoginDate", (new Date()).getTime());
        editor.putLong("totalReadingsProducedCounter", totalReadingsProducedCounter);
        editor.apply();
    }


    public String getSensorRules() {
//        String sensorRules = "";
//        for (ContextPluginInformation plugin : DynamixService.getAllContextPluginInfo()) {
//            if (plugin.getInstallStatus() == PluginInstallStatus.INSTALLED) {
//                sensorRules = sensorRules + plugin.getPluginId() + ",";
//            }
//
//        }
//        return sensorRules;
        return "";
    }


    public void experimentPush(Experiment exp) {
        if (editor == null) {
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
            editor = pref.edit();
        }
        String experimentsJson = pref.getString("experiments", "");
        if (experimentsJson != null && experimentsJson.length() > 0) {
            Type listType = new TypeToken<ArrayList<Experiment>>() {
            }.getType();
            experiments = (new Gson()).fromJson(experimentsJson, listType);
        }
        if (experiments == null) {
            experiments = new ArrayList<>();
        }
        experiments.add(exp);
        experimentsJson = (new Gson()).toJson(experiments);
        editor.putString("experiments", experimentsJson);
        editor.apply();
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    // keep stats for total time connected to the service

    public void setLastOnlineLogin() {

        Date dat = new Date();

        if (editor == null) {
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
            editor = pref.edit();
        }

        editor.putLong("lastOnlineLoginDate", dat.getTime());
        editor.apply();

    }

    public Date getLastOnlineLogin() {

        Date lastLoginDate;

        if (editor == null) {
            pref = AppModel.instance.context.getSharedPreferences("OrganicityConfigurations", 0);
        }

        lastLoginDate = new Date(pref.getLong("lastOnlineLoginDate", 0));

        return lastLoginDate;
    }

    public void incMsgCounter() {
        this.totalReadingsProducedCounter++;
    }

    public Long getTotalReadingsProduced() {
        return this.totalReadingsProducedCounter;
    }
}
