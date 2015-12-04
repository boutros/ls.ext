package no.deichman.services.search;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Responsibility: An embedded elasticsearch server for test purposes.
 */
public class EmbeddedElasticsearchServer {
    private static final String DEFAULT_DATA_DIRECTORY = "/var/tmp/target/elasticsearch-data";
    private final String dataDirectory;
    private Node node;

    public EmbeddedElasticsearchServer() {
        this(getTempDirectory());
        //this(DEFAULT_DATA_DIRECTORY);
    }

    public EmbeddedElasticsearchServer(String dataDirectory) {
        this.dataDirectory = dataDirectory;

        //new File(dataDirectory).mkdirs();
        Settings.Builder elasticsearchSettings;
        elasticsearchSettings = Settings.settingsBuilder()
                .put("http.enabled", "true")
                .put("path.home", ".")
                .put("path.data", dataDirectory);

        node = nodeBuilder()
                .local(true)
                .settings(elasticsearchSettings.build())
                .node();

        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(getClass().getResourceAsStream("/elasticsearch.json"), writer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read elasticsearch.json");
        }

        try {
            HttpResponse<JsonNode> response = Unirest.post("http://127.0.0.1:9200/search").body(writer.toString()).asJson();
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new RuntimeException("Unexpected status from Elasticsearch: " + response.getBody().toString());
            }
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTempDirectory() {
        try {
            Path tempDir = Files.createTempDirectory("elasticsearch-data");
            return tempDir.toAbsolutePath().toString();
        } catch (IOException e1) {
            throw new RuntimeException("Could not create temp directory for embedded Elasticsearch");
        }
    }

    public final Client getClient() {
        return node.client();
    }

    public final void shutdown() {
        node.close();
        deleteDataDirectory();
    }

    private void deleteDataDirectory() {
        try {
            FileUtils.deleteDirectory(new File(dataDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete data directory of embedded elasticsearch server", e);
        }
    }
}
