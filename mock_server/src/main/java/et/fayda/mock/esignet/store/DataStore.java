package et.fayda.mock.esignet.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import et.fayda.mock.esignet.model.AuthTransaction;
import et.fayda.mock.esignet.model.MockIdentity;
import et.fayda.mock.esignet.model.OidcClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all server state. `clients` and `identities` persist to a JSON file
 * (see `persist()`/`init()`) so registrations survive a restart; transactions
 * are session state and stay in memory only.
 */
@Component
public class DataStore {

    public final Map<String, OidcClient> clients = new ConcurrentHashMap<>();
    public final Map<String, MockIdentity> identities = new ConcurrentHashMap<>(); // key: individualId
    public final Map<String, AuthTransaction> transactionsById = new ConcurrentHashMap<>();
    public final Map<String, AuthTransaction> transactionsByCode = new ConcurrentHashMap<>();
    public final Map<String, AuthTransaction> transactionsByAccessToken = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storeFile;

    public DataStore(@Value("${mock.store.file:data/mock-store.json}") String storeFilePath) {
        this.storeFile = Path.of(storeFilePath);
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(storeFile) || Files.size(storeFile) == 0) {
                seedDemoIdentities();
                persist();
                return;
            }
            StoreFile loaded = objectMapper.readValue(storeFile.toFile(), StoreFile.class);
            for (OidcClient client : loaded.clients) {
                clients.put(client.clientId, client);
            }
            for (MockIdentity identity : loaded.identities) {
                identities.put(identity.individualId, identity);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock store from " + storeFile, e);
        }
    }

    /** Serializes clients + identities to disk. Called after every admin write. */
    public synchronized void persist() {
        StoreFile snapshot = new StoreFile();
        snapshot.clients = new ArrayList<>(clients.values());
        snapshot.identities = new ArrayList<>(identities.values());
        try {
            if (storeFile.getParent() != null) {
                Files.createDirectories(storeFile.getParent());
            }
            Path tmp = storeFile.resolveSibling(storeFile.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), snapshot);
            Files.move(tmp, storeFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist mock store to " + storeFile, e);
        }
    }

    private void seedDemoIdentities() {
        MockIdentity abebe = new MockIdentity("6140798523917519", "1234");
        abebe.name.put("en", "Abebe Kebede");
        abebe.name.put("am", "አበበ ከበደ");
        abebe.email = "abebe.kebede@example.com";
        abebe.phoneNumber = "+251911223344";
        abebe.gender = "Male";
        abebe.birthdate = "1990-05-14";
        abebe.picture = "https://placehold.co/150x150?text=Abebe";
        abebe.region.put("en", "Addis Ababa");
        abebe.region.put("am", "አዲስ አበባ");
        abebe.zone.put("en", "Bole");
        abebe.zone.put("am", "ቦሉ");
        abebe.woreda.put("en", "Woreda 03");
        abebe.woreda.put("am", "ወረዳ 03");
        abebe.kebele.put("en", "Kebele 05");
        abebe.kebele.put("am", "ቀቤሌ 05");
        identities.put(abebe.individualId, abebe);

        MockIdentity hanna = new MockIdentity("2938475610283746", "5678");
        hanna.name.put("en", "Hanna Girma");
        hanna.name.put("am", "ሀና ግርማ");
        hanna.email = "hanna.girma@example.com";
        hanna.phoneNumber = "+251922334455";
        hanna.gender = "Female";
        hanna.birthdate = "1995-11-02";
        hanna.picture = "https://placehold.co/150x150?text=Hanna";
        hanna.region.put("en", "Oromia");
        hanna.region.put("am", "ኦሮሚያ");
        hanna.zone.put("en", "East Shewa");
        hanna.zone.put("am", "ምስራቅ ሻዋ");
        hanna.woreda.put("en", "Adama");
        hanna.woreda.put("am", "አዳማ");
        hanna.kebele.put("en", "Kebele 02");
        hanna.kebele.put("am", "ቀቤሌ 02");
        identities.put(hanna.individualId, hanna);
    }

    /** On-disk shape for the persisted store file - not a domain model. */
    private static final class StoreFile {
        public List<OidcClient> clients = new ArrayList<>();
        public List<MockIdentity> identities = new ArrayList<>();
    }
}
