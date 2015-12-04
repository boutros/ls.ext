package no.deichman.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import no.deichman.services.search.EmbeddedElasticsearchServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class SearchTest {
    public static final int ONE_SECOND = 1000;
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int FIVE = 5;
    public static final int TEN_TIMES = 10;
    private static final Logger LOG = LoggerFactory.getLogger(AppTest.class);
    private static EmbeddedElasticsearchServer embeddedElasticsearchServer;
    private static String elasticSearchUrl = System.getProperty("ELASTCSEARCH_URL", "http://localhost:9200");

    @BeforeClass
    public static void setup() throws Exception {
        setupElasticSearch();
        populateSearchIndex();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        embeddedElasticsearchServer.shutdown();
    }

    private static void setupElasticSearch() throws Exception {
        embeddedElasticsearchServer = new EmbeddedElasticsearchServer();
    }

    private static String generateWorkJson(String title, String creator) {
        JsonObject json = new JsonObject();
        if (title != null) {
            json.addProperty("title", title);
        }
        if (creator != null) {
            JsonObject name = new JsonObject();
            name.addProperty("name", creator);
            json.add("creator", name);
        }

        return json.toString();
    }

    private static void indexDocument(String type, String jsonData) {
        embeddedElasticsearchServer.getClient().prepareIndex("search", type)
                .setSource(jsonData)
                .execute()
                .actionGet();
    }

    private static void populateSearchIndex() {
        indexDocument("work", generateWorkJson("When the Last Acorn is Found", "Deborah Latzke"));
        indexDocument("work", generateWorkJson("Lost. Found.", "Marsha Diane Arnold"));
        indexDocument("work", generateWorkJson("When I Found You", "Catherine Ryan Hyde"));
        indexDocument("work", generateWorkJson("I", "Anonymous"));
        indexDocument("work", generateWorkJson("I, robot", "Isaac Asimov"));
        indexDocument("work", generateWorkJson("Of Cowards and True Men", "Rob Miech"));
        indexDocument("work", generateWorkJson("Apocalypse Cow", "Michael Logan"));
        indexDocument("work", generateWorkJson("Know Your Cows", "Jack Byard"));
        indexDocument("work", generateWorkJson("Cow", "Malachy Doyle"));
        indexDocument("work", generateWorkJson("Hamsun. Erobreren", "Ingar Sletten Kolloen"));
        indexDocument("work", generateWorkJson("Gåten Knut Hamsun", "Robert Ferguson"));
        indexDocument("work", generateWorkJson("Knut Hamsun", "Einar Skavlan"));
        indexDocument("work", generateWorkJson("Sult", "Knut Hamsun"));
        indexDocument("work", generateWorkJson("Sværmere", "Knut Hamsun"));

        indexDocument("work", generateWorkJson("Cats With Guns", "Jonathan Parkyn"));
        indexDocument("work", generateWorkJson("V for Vendetta", "David Lloyd"));
    }

    public com.google.gson.JsonArray searchDocument(String type, String query, int numberOfExpectedResults) throws UnirestException, InterruptedException {
        return searchDocument(type, query, numberOfExpectedResults, false);
    }

    public com.google.gson.JsonArray searchDocument(String type, String query, int numberOfExpectedResults, boolean jsonInput) throws UnirestException, InterruptedException {
        int attempts = TEN_TIMES;
        do {
            HttpRequest request;
            if (jsonInput) {
                request = Unirest.post(elasticSearchUrl + "/search/" + type + "/_search").body(query).getHttpRequest();
            } else {
                request = Unirest.get(elasticSearchUrl + "/search/" + type + "/_search").queryString("q", query);
            }
            HttpResponse<?> response = request.asJson();
            JsonObject json = new Gson().fromJson(response.getBody().toString(), JsonObject.class);
            com.google.gson.JsonArray results = new com.google.gson.JsonArray();
            json.getAsJsonObject("hits").getAsJsonArray("hits").forEach(hit -> results.add(hit.getAsJsonObject().get("_source")));
            if (results.size() != numberOfExpectedResults) {
                Thread.sleep(ONE_SECOND);
            } else {
                return results;
            }
        } while (attempts-- > 0);
        fail("Should have found the correct number of hits by now.");
        return null; // Will never get here
    }

    @Test
    public void return_hits_when_three_or_more_characters_in_a_word_matches() throws InterruptedException, UnirestException {
        com.google.gson.JsonArray results = searchDocument("work", "cow", FOUR);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Apocalypse Cow", null));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("Cow", null));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("Know Your Cows", null));
        assertJsonSimilar(results.get(THREE).toString(), generateWorkJson("Of Cowards and True Men", null));

    }

    @Test
    public void return_hits_with_single_character_in_title() throws UnirestException, InterruptedException {
        com.google.gson.JsonArray results = searchDocument("work", "i", THREE);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("I", null));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("I, robot", null));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("When I Found You", null));
    }

    @Test
    public void return_hits_when_searching_for_multiple_words() throws InterruptedException, UnirestException {
        com.google.gson.JsonArray results = searchDocument("work", "when found", THREE);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("When I Found You", null));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("When the Last Acorn is Found", null));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("Lost. Found.", null));

        results = searchDocument("work", "found when", THREE);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("When I Found You", null));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("When the Last Acorn is Found", null));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("Lost. Found.", null));
    }

    @Test
    public void return_hits_with_words_that_match_three_or_more_characters_in_order() throws InterruptedException, UnirestException {
        com.google.gson.JsonArray results = searchDocument("work", "cow", FOUR);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Cow", null));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("Apocalypse Cow", null));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("Know Your Cows", null));
        assertJsonSimilar(results.get(THREE).toString(), generateWorkJson("Of Cowards and True Men", null));

        results = searchDocument("work", "cows", ONE);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Know Your Cows", null));

        results = searchDocument("work", "ows", ONE);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Know Your Cows", null));

        results = searchDocument("work", "pocalyps", ONE);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Apocalypse Cow", null));
    }

    @Test
    public void return_hits_matching_creator_name_over_hits_matching_hitle() throws InterruptedException, UnirestException {
        com.google.gson.JsonArray results = searchDocument("work", generateJsonQuery("hamsun"), FIVE, true);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Sult", "Knut Hamsun"));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("Sværmere", "Knut Hamsun"));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("Hamsun. Erobreren", "Ingar Sletten Kolloen"));
        assertJsonSimilar(results.get(THREE).toString(), generateWorkJson("Knut Hamsun", "Einar Skavlan"));
        assertJsonSimilar(results.get(FOUR).toString(), generateWorkJson("Gåten Knut Hamsun", "Robert Ferguson"));

        results = searchDocument("work", generateJsonQuery("knut hamsun"), FIVE, true);
        assertJsonSimilar(results.get(ZERO).toString(), generateWorkJson("Sult", "Knut Hamsun"));
        assertJsonSimilar(results.get(ONE).toString(), generateWorkJson("Sværmere", "Knut Hamsun"));
        assertJsonSimilar(results.get(TWO).toString(), generateWorkJson("Knut Hamsun", "Einar Skavlan"));
        assertJsonSimilar(results.get(THREE).toString(), generateWorkJson("Gåten Knut Hamsun", "Robert Ferguson"));
        assertJsonSimilar(results.get(FOUR).toString(), generateWorkJson("Hamsun. Erobreren", "Ingar Sletten Kolloen"));
    }

    private String generateJsonQuery(String query) {
        return "{  \n"
                + "   \"query\":{  \n"
                + "      \"bool\":{  \n"
                + "         \"should\":[  \n"
                + "            {  \n"
                + "               \"nested\":{  \n"
                + "                  \"path\":\"creator\",\n"
                + "                  \"query\":{  \n"
                + "                     \"match\":{  \n"
                + "                        \"creator.name\":{  \n"
                + "                           \"query\":\"" + query + "\"\n"
                + "                        }\n"
                + "                     }\n"
                + "                  }\n"
                + "               }\n"
                + "            },\n"
                + "            {  \n"
                + "               \"match\":{  \n"
                + "                  \"title\":{  \n"
                + "                     \"query\":\"" + query + "\"\n"
                + "                  }\n"
                + "               }\n"
                + "            }\n"
                + "         ]\n"
                + "      }\n"
                + "   }\n"
                + "}";
    }

    private void assertJsonSimilar(String actual, String expected) {
        assertThat(actual, sameJSONAs(expected).allowingExtraUnexpectedFields().allowingAnyArrayOrdering());
    }
}
