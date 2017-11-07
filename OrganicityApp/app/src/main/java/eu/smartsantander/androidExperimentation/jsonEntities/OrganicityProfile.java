package eu.smartsantander.androidExperimentation.jsonEntities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An object describing the Organicity Profile response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganicityProfile {

    private String name;
    private String sub;
    private String preferred_username;
    private String given_name;
    private String family_name;
    private String email;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(final String sub) {
        this.sub = sub;
    }

    public String getPreferred_username() {
        return preferred_username;
    }

    public void setPreferred_username(final String preferred_username) {
        this.preferred_username = preferred_username;
    }

    public String getGiven_name() {
        return given_name;
    }

    public void setGiven_name(final String given_name) {
        this.given_name = given_name;
    }

    public String getFamily_name() {
        return family_name;
    }

    public void setFamily_name(final String family_name) {
        this.family_name = family_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "OrganicityProfile{" +
                "name='" + name + '\'' +
                ", sub='" + sub + '\'' +
                ", preferred_username='" + preferred_username + '\'' +
                ", given_name='" + given_name + '\'' +
                ", family_name='" + family_name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
