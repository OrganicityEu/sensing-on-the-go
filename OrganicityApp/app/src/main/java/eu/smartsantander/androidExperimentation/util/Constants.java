package eu.smartsantander.androidExperimentation.util;

import java.io.File;

import eu.organicity.set.app.operations.Downloader;

public class Constants {
    public static final String RESULT_ACTION = "gr.cti.ru1.organicity.set.actions.RESULT_ACTION";
    public static final String MEASUREMENT_LOCATION_ACTION = "gr.cti.ru1.organicity.set.actions.MEASUREMENT_LOCATION_ACTION";
    public static final String RESULT_EXTRA= "result";

    public static final String EXPERIMENT_PLUGIN_CONTEXT_TYPE = "org.ambientdynamix.contextplugins.ExperimentPlugin";
    public static String URL = "http://set.organicity.eu:8080";
    public static final int PHONE_ID_UNITIALIZED = -1;
    public static String activityStatus = "unknown";
    public static final long EXPERIMENT_POLL_INTERVAL = 15000;

    public static final String EXPERIMENT_STATE_INTENT = "organicity-experiment-state";
    public static final String EXPERIMENTS = "experiments";

    public static final String EXPERIMENT = "EXPERIMENT";
    public static final String STATE = "STATE";


    // OC AAA params
    public static final String ORGANICITY_APP_CALLBACK_OAUTHCALLBACK = "https://set.organicity.eu/oauth/complete";
    public static final String ORGANICITY_APP_OAUTH_BASEURL = "https://accounts.organicity.eu/realms/organicity/protocol/openid-connect";
    public static final String ORGANICITY_APP_OAUTH_TOKENURL = ORGANICITY_APP_OAUTH_BASEURL + "/token";
    public static final String ORGANICITY_APP_OAUTH_URL = "/auth";

    public static void checkFile(String filename, String url) throws Exception {
        File root = android.os.Environment.getExternalStorageDirectory();
        File myfile = new File(root.getAbsolutePath() + "/dynamix/" + filename);

        if (!myfile.exists()) {
            Downloader downloader = new Downloader();
            downloader.DownloadFromUrl(url, filename);
        }
    }

    static public void checkExperiment(String contextType, String url) throws Exception {
        File root = android.os.Environment.getExternalStorageDirectory();
        File myfile = new File(root.getAbsolutePath() + "/dynamix/" + contextType);

        if (!myfile.exists()) {
            Downloader downloader = new Downloader();
            downloader.DownloadFromUrl(url, contextType);
        }
    }

    public static boolean match(String[] smartphoneDependencies, String[] experimentDependencies) {
        for (String expDependency : experimentDependencies) {
            boolean found = false;
            for (String smartphoneDependency : smartphoneDependencies) {
                if (smartphoneDependency.equals(expDependency)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

}



