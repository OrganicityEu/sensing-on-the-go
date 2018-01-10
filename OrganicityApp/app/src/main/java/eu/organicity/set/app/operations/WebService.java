package eu.organicity.set.app.operations;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;

import eu.organicity.client.OrganicityServiceBaseClient;
import eu.organicity.discovery.dto.FeatureCollectionDTO;
import gr.cti.android.experimentation.model.ExperimentDTO;
import gr.cti.android.experimentation.model.ExperimentListDTO;
import gr.cti.android.experimentation.model.NewAssetDTO;
import gr.cti.android.experimentation.model.PluginListDTO;
import gr.cti.android.experimentation.model.RegionListDTO;
import gr.cti.android.experimentation.model.ResultDTO;
import gr.cti.android.experimentation.model.SmartphoneDTO;
import gr.cti.android.experimentation.model.SmartphoneStatisticsDTO;

public class WebService extends OrganicityServiceBaseClient {
    private static final String BASE_URL = "https://api.smartphone-experimentation.eu/";
    private static final String ACCOUNTS_TOKEN_ENDPOINT = "https://accounts.organicity.eu/realms/organicity/protocol/openid-connect/token";
    private String encodedToken;

    public WebService() {
        this("");
    }

    public WebService(String token) {
        super(token);
    }

    public void setErrorHandler(ResponseErrorHandler responseErrorHandler) {
        this.restTemplate.setErrorHandler(responseErrorHandler);
    }

    public void clearToken() {
        this.headers.remove("Authorization");
    }

    public void setEncodedToken(String encodedToken) {
        this.encodedToken = encodedToken;
    }

    public boolean updateAccessToken() {
        return true;
    }

    public ExperimentListDTO listExperiments() {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (ExperimentListDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/api/v1/experiment", HttpMethod.GET, this.req, ExperimentListDTO.class, new Object[0]).getBody();
    }

    public ExperimentListDTO listLiveExperiments() {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (ExperimentListDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/experiment/live", HttpMethod.GET, this.req, ExperimentListDTO.class, new Object[0]).getBody();
    }

    public ExperimentListDTO listExperiments(int smartphoneId) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (ExperimentListDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/experiment?phoneId=" + smartphoneId, HttpMethod.GET, this.req, ExperimentListDTO.class, new Object[0]).getBody();
    }

    public ExperimentDTO getExperiment(String experimentId) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (ExperimentDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/experiment/" + experimentId, HttpMethod.GET, this.req, ExperimentDTO.class, new Object[0]).getBody();
    }

    public PluginListDTO listPlugins() {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (PluginListDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/api/v1/plugin", HttpMethod.GET, this.req, PluginListDTO.class, new Object[0]).getBody();
    }

    public SmartphoneStatisticsDTO getSmartphoneStatistics(int smartphoneId) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (SmartphoneStatisticsDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/smartphone/" + smartphoneId + "/statistics", HttpMethod.GET, this.req, SmartphoneStatisticsDTO.class, new Object[0]).getBody();
    }

    public SmartphoneStatisticsDTO getSmartphoneStatistics(int smartphoneId, String experimentId) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (SmartphoneStatisticsDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/smartphone/" + smartphoneId + "/statistics/" + experimentId, HttpMethod.GET, this.req, SmartphoneStatisticsDTO.class, new Object[0]).getBody();
    }

    public SmartphoneDTO postSmartphone(SmartphoneDTO smartphone) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (SmartphoneDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/smartphone", HttpMethod.POST, new HttpEntity(smartphone, this.headers), SmartphoneDTO.class, new Object[0]).getBody();
    }

    public RegionListDTO getExperimentRegions(String experimentId) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return (RegionListDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/experiment/" + experimentId + "/region", HttpMethod.GET, this.req, RegionListDTO.class, new Object[0]).getBody();
    }

    public String postExperimentResults(ResultDTO resultListDTO) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        return this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/data", HttpMethod.POST, new HttpEntity(resultListDTO, this.headers), String.class, new Object[0]).getBody();
    }

    public NewAssetDTO sendAsset(String assetName, String assetType, String experimentId, double latitude, double longitude) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        NewAssetDTO newAssetDTO = new NewAssetDTO();
        newAssetDTO.setName(assetName);
        newAssetDTO.setType(assetType);
        newAssetDTO.setExperimentId(experimentId);
        newAssetDTO.setLatitude(latitude);
        newAssetDTO.setLongitude(longitude);
        return (NewAssetDTO)this.restTemplate.exchange("https://api.smartphone-experimentation.eu/v1/asset/add", HttpMethod.POST, new HttpEntity(newAssetDTO, this.headers), NewAssetDTO.class, new Object[0]).getBody();
    }

    public FeatureCollectionDTO[] listNearbyAssets(double lat, double lon) {
        if(!"".equals(this.getToken())) {
            this.updateAccessToken();
        }

        LinkedMultiValueMap codeMap = new LinkedMultiValueMap();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        HttpEntity internalRec = new HttpEntity(codeMap, headers);
        return (FeatureCollectionDTO[])this.restTemplate.exchange("https://api.smartphone-experimentation.eu/assets/geo/search?lat=" + lat + "&long=" + lon + "&radius=20", HttpMethod.GET, internalRec, FeatureCollectionDTO[].class, new Object[0]).getBody();
    }
}
