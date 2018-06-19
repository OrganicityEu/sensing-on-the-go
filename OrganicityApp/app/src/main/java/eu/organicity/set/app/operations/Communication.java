package eu.organicity.set.app.operations;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.organicity.discovery.dto.FeatureCollectionDTO;
import eu.organicity.set.app.BuildConfig;
import eu.organicity.set.app.sdk.Report;
import eu.organicity.set.app.utils.AccountUtils;
import eu.organicity.set.app.utils.UserDTO;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;
import eu.smartsantander.androidExperimentation.util.Constants;
import eu.smartsantander.androidExperimentation.util.MyResponseErrorHandler;
import eu.smartsantander.androidExperimentation.util.OauthTokenResponse;
import gr.cti.android.experimentation.model.ExperimentDTO;
import gr.cti.android.experimentation.model.ExperimentListDTO;
import gr.cti.android.experimentation.model.NewAssetDTO;
import gr.cti.android.experimentation.model.PluginDTO;
import gr.cti.android.experimentation.model.PluginListDTO;
import gr.cti.android.experimentation.model.RegionListDTO;
import gr.cti.android.experimentation.model.ResultDTO;
import gr.cti.android.experimentation.model.SmartphoneDTO;
import gr.cti.android.experimentation.model.SmartphoneStatisticsDTO;

public class Communication extends Thread implements Runnable {
    public static final String OC_ACCOUNTS_PREF_NAME = "ocaccounts";
    public static final String OC_REF_TOKEN_NAME = "refresh_token";
    //private Handler handler;
    private final String TAG = this.getClass().getSimpleName();

    final RestTemplate restTemplate = new RestTemplate();
    private final MultiValueMap<String, String> refreshTokenMap;
    private final MultiValueMap<String, String> codeMap;
    final HttpHeaders emptyHeaders = new HttpHeaders();

    static WebService webServiceClient = new WebService();
    private int lastHash;
    private String message;

    private static Communication instance;

    public static Communication getInstance() {
        if (instance ==  null) {
            instance = new Communication();
        }

        return instance;
    }

    public Communication() {

        webServiceClient.setErrorHandler(new MyResponseErrorHandler());
        restTemplate.setErrorHandler(new MyResponseErrorHandler());
        webServiceClient.setEncodedToken(BuildConfig.OC_ENC_ID);

        // Add the String message converter
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        codeMap = new LinkedMultiValueMap<>();
        codeMap.add("client_id", BuildConfig.OC_APP_ID);
        codeMap.add("client_secret", BuildConfig.OC_APP_SECRET);
        codeMap.add("grant_type", "authorization_code");

        refreshTokenMap = new LinkedMultiValueMap<>();
        refreshTokenMap.add("client_id", BuildConfig.OC_APP_ID);
        refreshTokenMap.add("client_secret", BuildConfig.OC_APP_SECRET);
        refreshTokenMap.add("grant_type", "refresh_token");
        refreshTokenMap.add("scope", "openid%20profile");

        lastHash = 0;
    }


    public void disconnectUser() {
        webServiceClient.clearToken();
    }

    /**
     * Register a smartphone to the server.
     *
     * @param phoneId      the unique id of the smartphone.
     * @param sensorsRules a list of sensors available on the phone.
     * @return the server id of the smartphone.
     * @throws Exception
     */
    public int registerSmartphone(final int phoneId, final String sensorsRules) throws Exception {
        Log.i(TAG, "Registering phone " + phoneId + " rules:" + sensorsRules);
        final SmartphoneDTO smartphone = new SmartphoneDTO();
        smartphone.setPhoneId((long) phoneId);
        smartphone.setSensorsRules(sensorsRules);

        try {
            return doRegisterSmartphone(smartphone);
        } catch (RestClientException e) {
            return 0;
        }
    }

    private int doRegisterSmartphone(final SmartphoneDTO smartphone) {
        int serverPhoneId = 0;
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            final SmartphoneDTO smartphoneReceived = webServiceClient.postSmartphone(smartphone);
            if (smartphoneReceived != null) {
                serverPhoneId = smartphoneReceived.getId();
            } else {
                serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
            }
        } catch (Exception e) {
            serverPhoneId = Constants.PHONE_ID_UNITIALIZED;
            Log.e(TAG, "Device Registration Exception:" + e.getMessage(), e);
        }
        return serverPhoneId;
    }


    public SmartphoneStatisticsDTO getSmartphoneStatistics(Integer id) {
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return webServiceClient.getSmartphoneStatistics(id);
        } catch (RestClientException e) {
            return null;
        }
    }

    public SmartphoneStatisticsDTO getSmartphoneStatistics(int id, String experimentId) {
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            SmartphoneStatisticsDTO dto = webServiceClient.getSmartphoneStatistics(id, experimentId);
            if (dto != null) {

            }
            return dto;
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public RegionListDTO getExperimentRegions(String experimentId) {
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return webServiceClient.getExperimentRegions(experimentId);
        } catch (RestClientException e) {
            return null;
        }
    }

    /**
     * Get the last points of measurements by the user.
     * TODO : move it to the backend for statistics
     *
     * @param phoneId the if of the user's phone.
     * @return
     */
    public JSONArray getLastPoints(final int phoneId) {
        final String path = "/data?deviceId=" + phoneId + "&after=today";
        try {
            final String stats = get(path);
            try {
                return new JSONArray(stats);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieve a list of all the available experiments.
     *
     * @return a list of experiments the user can install on his smartphone.
     * @throws Exception
     */
    public List<Experiment> getExperiments() throws Exception {
        return doGetExperiments();
    }

    private List<Experiment> doGetExperiments() {
        String token = AccountUtils.updateAccessToken();
        if (token != null) {
            webServiceClient.setToken(AccountUtils.updateAccessToken());
        }

        final PluginListDTO plugins = webServiceClient.listPlugins();
        final HashMap<String, Sensor> sensors = new HashMap<>();
        for (final PluginDTO dto : plugins.getPlugins()) {
            final Sensor s = new Sensor(dto.getName(), dto.getContextType(), dto.getFilename());

            sensors.put(dto.getContextType(), s);
        }


        final ExperimentListDTO experiments = webServiceClient.listLiveExperiments();
        final ArrayList<Experiment> internalExperiments = new ArrayList<>();
        for (final ExperimentDTO dto : experiments.getExperiments()) {
            Experiment exp = new Experiment();

            exp.setId(dto.getId());
            exp.setName(dto.getName());
            exp.setDescription(dto.getDescription());
            UserDTO[] users = webServiceClient.getUser(dto.getUserId());
            if (users.length>0) {
                exp.setUserId(users[0].getUsername());
            } else {
                exp.setUserId(dto.getUserId());
            }
            exp.setService(dto.getFilename());
            exp.setTimestamp(dto.getTimestamp());
            exp.setPkg(dto.getContextType());
            exp.createKey();
            exp.setSensorDependencies(dto.getSensorDependencies());
            exp.setStatus(dto.getStatus());
            exp.setUrl(dto.getUrl());
            exp.setUrlDescription(dto.getUrlDescription());
            exp.setParentExperimentId(dto.getParentExperimentId());

            List<Sensor> expSensors = new ArrayList<>();
            for (String s : dto.getSensorDependencies().split(",")) {
                expSensors.add(sensors.get(s));
            }
            exp.setSensors(expSensors);

            internalExperiments.add(exp);
        }
        return internalExperiments;
    }

    /**
     * Retrieve a list of all the available sensors.
     *
     * @return a list of experiments the user can install on his smartphone.
     * @throws Exception
     */
    public List<Sensor> getSensors() throws Exception {
        return doGetSensors();
    }

    private List<Sensor> doGetSensors() {
        String token = AccountUtils.updateAccessToken();
        if (token != null) {
            webServiceClient.setToken(AccountUtils.updateAccessToken());
        }

        final PluginListDTO plugins = webServiceClient.listPlugins();
        final ArrayList<Sensor> sensors = new ArrayList<>();
        for (final PluginDTO dto : plugins.getPlugins()) {
            final Sensor s = new Sensor(dto.getName(), dto.getContextType(), dto.getFilename());

            sensors.add(s);
        }

        return sensors;
    }


    /**
     * Retrieve a list of all the available experiments for the given phoneId.
     *
     * @param phoneId the phoneId that queries for experiments.
     * @return a list of experiments the user can install on his smartphone.
     * @throws Exception
     */
    public List<Experiment> getExperimentsById(final String phoneId) throws Exception {

        URI targetUrl = UriComponentsBuilder.fromUriString(Constants.URL).path("/api/v1/experiment").queryParam("phoneId", phoneId).build().toUri();

        String experimentsString = get(targetUrl);
        return new ObjectMapper().readValue(experimentsString, new TypeReference<List<Experiment>>() {
        });
    }

    /**
     * Report a set of results to the server.
     *
     * @param jsonReport a json text representation of the response.
     * @return
     * @throws Exception
     */
    public int sendReportResults(String jsonReport) throws Exception {
        //do not send them twice
        if (jsonReport.hashCode() == lastHash) {
            return 0;
        }

        //TODO replace this?
//        DynamixService.logToFile(jsonReport);
        try {
            postResults("/data", jsonReport);
            lastHash = jsonReport.hashCode();
            return 0;
        } catch (HttpClientErrorException e) {
            //ignore
            return 0;

        }
    }

    public String sendReportResults(final Report jsonReport) throws Exception {
        try {
            Log.i(TAG, "sendReportResults :" + jsonReport.getExperimentId());
            String res = postResults(jsonReport);
            lastHash = jsonReport.hashCode();
            return res;
        } catch (HttpClientErrorException e) {
            //ignore
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieve a list of all the available plugins for the given phoneId.
     *
     * @param phoneId the phoneId that queries for plugins.
     * @return a list of plugins the user can install on his smartphone.
     * @throws Exception
     */
    public List<PluginDTO> sendGetPluginList(final String phoneId) throws Exception {
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return webServiceClient.listPlugins().getPlugins();
        } catch (RestClientException e) {
            return new ArrayList<>();
        }
    }

    private String postResults(final String path, final String entity) throws Exception {

        final String url = Constants.URL + "/api/v1" + path;
        // Make the HTTP POST request, marshaling the response to a String
        return restTemplate.postForObject(url, entity, String.class);
    }

    private String postResults(final Report result) throws Exception {
        ResultDTO dto = new ResultDTO();
        dto.setJobResults(result.getJobResults());
        dto.setExperimentId(result.getExperimentId());
        dto.setDeviceId((long) result.getDeviceId());
//        dto.getResultList().add(result);
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return webServiceClient.postExperimentResults(dto);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String get(final String path) throws Exception {

        final String url = Constants.URL + "/api/v1" + path;

        // Make the HTTP GET request, marshaling the response to a String
        return restTemplate.getForObject(url, String.class, new ArrayList<String>());
    }

    private String get(final URI uri) throws Exception {

        // Make the HTTP GET request, marshaling the response to a String
        return restTemplate.getForObject(uri, String.class);
    }

    public void setLastMessage(String message) {
        this.message = message;
    }

    public String getLastMessage() {
        return this.message;
    }


    public boolean getToken(final String code, final String redirectUri, Context context) {
        try {

            codeMap.add("code", code);
            codeMap.add("redirect_uri", redirectUri);
            codeMap.add("grant_type", "authorization_code");

            final HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(codeMap, emptyHeaders);
            final ResponseEntity<OauthTokenResponse> response = restTemplate.exchange(Constants.ORGANICITY_APP_OAUTH_TOKENURL, HttpMethod.POST, req, OauthTokenResponse.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return storeOfflineToken(response.getBody(), context);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return false;
    }

    private boolean storeOfflineToken(final OauthTokenResponse response, Context context) {
        try {
            AccountUtils.storeOfflineToken(response.getRefresh_token());
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return false;
    }

    public FeatureCollectionDTO[] listNearbyAssets(double latitude, double longitude) {
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return webServiceClient.listNearbyAssets(latitude, longitude);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return new FeatureCollectionDTO[]{};
        }
    }


    public NewAssetDTO sendAsset(String assetNameText, String assetType, String experimentId, Location lastKnownLocation) {
        Log.i(TAG, "name:" + assetNameText + " exp:" + experimentId + " loc:" + lastKnownLocation);
        try {
            String token = AccountUtils.updateAccessToken();
            if (token != null) {
                webServiceClient.setToken(AccountUtils.updateAccessToken());
            }
            return webServiceClient.sendAsset(assetNameText, assetType, experimentId, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
