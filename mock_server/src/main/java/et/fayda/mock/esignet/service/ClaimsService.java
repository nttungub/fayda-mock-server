package et.fayda.mock.esignet.service;

import et.fayda.mock.esignet.model.MockIdentity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the claims returned from /oidc/userinfo, matching the shape observed
 * across real Fayda relying-party integrations: name, email, phone_number, gender,
 * birthdate, picture, address (region/zone/woreda/kebele), individual_id, etc.
 *
 * If 2+ claims_locales are requested, localized fields get a "#<lang>" suffix
 * (name#en, name#am) instead of a plain key - matching eSignet's documented behavior.
 */
@Service
public class ClaimsService {

    private static final List<String> DEFAULT_LOCALE = List.of("en");

    public Map<String, Object> buildClaims(MockIdentity identity, List<String> requestedClaims, List<String> locales) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> useLocales = (locales == null || locales.isEmpty()) ? DEFAULT_LOCALE : locales;
        boolean multiLocale = useLocales.size() > 1;

        for (String claim : requestedClaims) {
            switch (claim) {
                case "name" -> putLocalized(out, "name", identity.name, useLocales, multiLocale);
                case "email" -> out.put("email", identity.email);
                case "phone_number" -> out.put("phone_number", identity.phoneNumber);
                case "phone_number_verified" -> out.put("phone_number_verified", true);
                case "gender" -> out.put("gender", identity.gender);
                case "birthdate" -> out.put("birthdate", identity.birthdate);
                case "picture" -> out.put("picture", identity.picture);
                case "individual_id" -> out.put("individual_id", identity.individualId);
                case "registration_type" -> out.put("registration_type", "MOSIP");
                case "updated_at" -> out.put("updated_at", System.currentTimeMillis() / 1000);
                case "address" -> out.put("address", buildAddress(identity, useLocales, multiLocale));
                default -> { /* unrecognized claim name - ignore */ }
            }
        }
        return out;
    }

    private void putLocalized(Map<String, Object> out, String key, Map<String, String> values,
                               List<String> locales, boolean multiLocale) {
        if (multiLocale) {
            for (String locale : locales) {
                String value = values.get(locale);
                if (value != null) {
                    out.put(key + "#" + locale, value);
                }
            }
        } else {
            String locale = locales.get(0);
            String value = values.getOrDefault(locale, values.values().stream().findFirst().orElse(null));
            out.put(key, value);
        }
    }

    private Map<String, Object> buildAddress(MockIdentity identity, List<String> locales, boolean multiLocale) {
        Map<String, Object> address = new LinkedHashMap<>();
        putLocalized(address, "region", identity.region, locales, multiLocale);
        putLocalized(address, "zone", identity.zone, locales, multiLocale);
        putLocalized(address, "woreda", identity.woreda, locales, multiLocale);
        putLocalized(address, "kebele", identity.kebele, locales, multiLocale);
        return address;
    }
}
