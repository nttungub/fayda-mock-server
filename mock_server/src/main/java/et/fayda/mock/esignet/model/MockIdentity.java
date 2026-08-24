package et.fayda.mock.esignet.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A fake citizen record used purely for local integration testing.
 * Every value here is fictional test data - it does not represent a real person
 * or a real Fayda/national ID number.
 *
 * Fields that can vary by language (name, region, zone, woreda, kebele) are stored
 * as locale -> value maps, e.g. {"en": "Abebe Kebede", "am": "\u12A0\u1260\u1260 \u12A8\u1260\u12F0"},
 * mirroring how eSignet returns name#en / name#am when multiple claims_locales are requested.
 */
public class MockIdentity {

    public final String individualId;
    public final String pin;

    public final Map<String, String> name = new LinkedHashMap<>();
    public String email;
    public String phoneNumber;
    public String gender;
    public String birthdate;
    public String picture;

    public final Map<String, String> region = new LinkedHashMap<>();
    public final Map<String, String> zone = new LinkedHashMap<>();
    public final Map<String, String> woreda = new LinkedHashMap<>();
    public final Map<String, String> kebele = new LinkedHashMap<>();

    @JsonCreator
    public MockIdentity(@JsonProperty("individualId") String individualId,
                         @JsonProperty("pin") String pin) {
        this.individualId = individualId;
        this.pin = pin;
    }
}
