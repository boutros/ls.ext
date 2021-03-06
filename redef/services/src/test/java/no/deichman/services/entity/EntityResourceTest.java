package no.deichman.services.entity;

import no.deichman.services.entity.kohaadapter.KohaAdapter;
import no.deichman.services.entity.kohaadapter.MarcRecord;
import no.deichman.services.entity.repository.InMemoryRepository;
import no.deichman.services.ontology.OntologyService;
import no.deichman.services.search.SearchService;
import no.deichman.services.uridefaults.BaseURI;
import org.apache.commons.lang.WordUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static no.deichman.services.entity.EntityServiceImplTest.modelForBiblio;
import static no.deichman.services.entity.repository.InMemoryRepositoryTest.repositoryWithDataFrom;
import static no.deichman.services.testutil.TestJSON.assertValidJSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityResourceTest {

    private static final String SOME_WORK_IDENTIFIER = "SOME_WORK_IDENTIFIER";
    private static final String SOME_OTHER_WORK_IDENTIFIER = "SOME_OTHER_WORK_IDENTIFIER";
    private static final String SOME_PERSON_IDENTIFIER = "SOME_PERSON_IDENTIFIER";
    private static final String SOME_PLACE_OF_PUBLICATION_IDENTIFIER = "SOME_PLACE_OF_PUBLICATION";
    private static final String WORK = "work";
    private static final String PERSON = "person";
    private static final String PLACE_OF_PUBLICATION = "placeOfPublication";
    private static final String PUBLICATION = "publication";
    private static final String A_BIBLIO_ID = "1234";
    private static final String LOCATION = "Location";
    private EntityResource entityResource;
    private BaseURI baseURI;

    @Mock
    private KohaAdapter mockKohaAdapter;

    @Mock
    private SearchService mockSearchService;

    @Mock
    private OntologyService mockOntologyService;

    @Before
    public void setUp() throws Exception {
        baseURI = BaseURI.local();
        EntityServiceImpl service = new EntityServiceImpl(baseURI, new InMemoryRepository(), mockKohaAdapter);
        entityResource = new EntityResource(baseURI, service, mockSearchService);
    }

    @Test
    public void should_have_default_constructor() {
        assertNotNull(new EntityResource());
    }

    @Test
    public void get_should_return_a_valid_json_work() {
        entityResource = new EntityResource(baseURI, new EntityServiceImpl(baseURI, repositoryWithDataFrom("testdata.ttl"), null), mockSearchService);
        String workId = "work_00001";

        Response result = entityResource.get(WORK, workId);

        assertNotNull(result);
        assertEquals(OK.getStatusCode(), result.getStatus());
        assertValidJSON(result.getEntity().toString());
    }

    @Test(expected = NotFoundException.class)
    public void get_should_throw_exception_when_work_is_not_found() {
        String workId = "work_DOES_NOT_EXIST";
        entityResource.get(WORK, workId);
    }

    @Test
    public void create_should_return_201_when_work_created() throws URISyntaxException {
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);
        Response result = entityResource.createFromLDJSON(WORK, work);

        assertNull(result.getEntity());
        assertEquals(CREATED.getStatusCode(), result.getStatus());
    }

    @Test
    public void create_should_return_201_when_person_created() throws URISyntaxException {
        String person = createTestRDF(SOME_PERSON_IDENTIFIER, PERSON);
        Response result = entityResource.createFromLDJSON(PERSON, person);

        assertNull(result.getEntity());
        assertEquals(CREATED.getStatusCode(), result.getStatus());
    }

    @Test
    public void create_should_return_201_when_place_of_publication_created() throws URISyntaxException {
        String placeOfPublication = createTestRDF(SOME_PLACE_OF_PUBLICATION_IDENTIFIER, PLACE_OF_PUBLICATION);
        Response result = entityResource.createFromLDJSON(PLACE_OF_PUBLICATION, placeOfPublication);

        assertNull(result.getEntity());
        assertEquals(CREATED.getStatusCode(), result.getStatus());
    }

    @Test
    public void create_duplicate_person_returns_409() throws URISyntaxException {
        String personId = "n019283";
        String person = createPersonImportRDF(SOME_PERSON_IDENTIFIER, PERSON, personId);
        Response result = entityResource.createFromLDJSON(PERSON, person);
        assertNull(result.getEntity());
        assertEquals(CREATED.getStatusCode(), result.getStatus());
        Response duplicate = entityResource.createFromLDJSON(PERSON, person);
        assertNotNull(duplicate.getEntity());
        assertEquals(CONFLICT.getStatusCode(), duplicate.getStatus());
    }

    @Test
    public void create_duplicate_place_of_publication_returns_409() throws URISyntaxException {
        String id = "g019283";
        String placeOfPublication = createPlaceOfPublicationImportRDF(SOME_PLACE_OF_PUBLICATION_IDENTIFIER, PLACE_OF_PUBLICATION, id);
        Response result = entityResource.createFromLDJSON(PLACE_OF_PUBLICATION, placeOfPublication);
        assertNull(result.getEntity());
        assertEquals(CREATED.getStatusCode(), result.getStatus());
        Response duplicate = entityResource.createFromLDJSON(PLACE_OF_PUBLICATION, placeOfPublication);
        assertNotNull(duplicate.getEntity());
        assertEquals(CONFLICT.getStatusCode(), duplicate.getStatus());
    }


    @Test
    public void create_should_return_location_header_when_work_created() throws URISyntaxException {
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);

        Response result = entityResource.createFromLDJSON(WORK, work);

        String workURI = baseURI.work();

        assertNull(result.getEntity());
        assertTrue(Pattern.matches(workURI + "w\\d{12}", result.getHeaderString(LOCATION)));
    }


    @Test
    public void create_should_return_location_header_when_person_created() throws URISyntaxException {
        String person = createTestRDF(SOME_PERSON_IDENTIFIER, PERSON);

        Response result = entityResource.createFromLDJSON(PERSON, person);

        String personURI = baseURI.person();

        assertNull(result.getEntity());
        assertTrue(Pattern.matches(personURI + "h\\d{12}", result.getHeaderString(LOCATION)));
    }

    @Test
    public void create_should_return_location_header_when_place_of_publication_created() throws URISyntaxException {
        String placeOfPublication = createTestRDF(SOME_PLACE_OF_PUBLICATION_IDENTIFIER, PLACE_OF_PUBLICATION);

        Response result = entityResource.createFromLDJSON(PLACE_OF_PUBLICATION, placeOfPublication);

        String placeOfPublicationURI = baseURI.placeOfPublication();

        assertNull(result.getEntity());
        assertTrue(Pattern.matches(placeOfPublicationURI + "g\\d{12}", result.getHeaderString(LOCATION)));
    }

    @Test
    public void update_should_return_200_when_work_updated() {
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);
        Response result = entityResource.update(WORK, work);

        assertNull(result.getEntity());
        assertEquals(OK.getStatusCode(), result.getStatus());
    }

    @Test
    public void create_should_return_the_new_work() throws URISyntaxException{
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);

        Response createResponse = entityResource.createFromLDJSON(WORK, work);

        String workId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/work/", "");

        Response result = entityResource.get(WORK, workId);

        assertNotNull(result);
        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());
        assertEquals(OK.getStatusCode(), result.getStatus());
        assertValidJSON(result.getEntity().toString());
    }

    @Test
    public void create_should_index_the_new_work() throws URISyntaxException{
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);

        Response createResponse = entityResource.createFromLDJSON(WORK, work);

        String workId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/work/", "");


        Response result = entityResource.index(WORK, workId);

        assertNotNull(result);
        assertEquals(ACCEPTED.getStatusCode(), result.getStatus());
    }

    @Test
    public void create_should_index_the_new_person() throws URISyntaxException{
        String person = createTestRDF(SOME_PERSON_IDENTIFIER, PERSON);

        Response createResponse = entityResource.createFromLDJSON(PERSON, person);

        String personId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/person/", "");


        Response result = entityResource.index(PERSON, personId);

        assertNotNull(result);
        assertEquals(ACCEPTED.getStatusCode(), result.getStatus());
    }

    @Test
    public void create_should_index_the_new_place_of_publication() throws URISyntaxException{
        String placeOfPublication = createTestRDF(SOME_PLACE_OF_PUBLICATION_IDENTIFIER, PLACE_OF_PUBLICATION);

        Response createResponse = entityResource.createFromLDJSON(PLACE_OF_PUBLICATION, placeOfPublication);

        String placeOfPublicationId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/placeOfPublication/", "");


        Response result = entityResource.index(PLACE_OF_PUBLICATION, placeOfPublicationId);

        assertNotNull(result);
        assertEquals(ACCEPTED.getStatusCode(), result.getStatus());
    }

    @Test
    public void get_work_items_should_return_list_of_items(){
        when(mockKohaAdapter.getBiblio("626460")).thenReturn(modelForBiblio());

        entityResource = new EntityResource(baseURI, new EntityServiceImpl(baseURI, repositoryWithDataFrom("testdata.ttl"), mockKohaAdapter), mockSearchService);

        String workId = "work_TEST_KOHA_ITEMS_LINK";

        Response result = entityResource.getWorkItems(workId, WORK);

        assertNotNull(result);
        assertEquals(OK.getStatusCode(), result.getStatus());
        assertValidJSON(result.getEntity().toString());
    }

    @Test
    public void get_creator_works_should_return_list_of_works(){
        entityResource = new EntityResource(baseURI, new EntityServiceImpl(baseURI, repositoryWithDataFrom("testdata.ttl"), mockKohaAdapter), mockSearchService);

        Response result = entityResource.getWorksByCreator("person_00001");

        assertNotNull(result);
        assertEquals(OK.getStatusCode(), result.getStatus());
        assertValidJSON(result.getEntity().toString());
    }

    @Test
    public void get_work_items_should_204_on_empty_items_list(){
        Response result = entityResource.getWorkItems("DOES_NOT_EXIST", WORK);
        assertEquals(result.getStatus(), NO_CONTENT.getStatusCode());
    }

    @Test
    public void patch_with_invalid_data_should_return_status_400() throws Exception {
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);
        Response result = entityResource.createFromLDJSON(WORK, work);
        String workId = result.getLocation().getPath().substring(("/" + WORK + "/").length());
        String patchData = "{}";
        try {
            entityResource.patch(WORK, workId,patchData);
            fail("HTTP 400 Bad Request");
        } catch (BadRequestException bre) {
            assertEquals("HTTP 400 Bad Request", bre.getMessage());
        }
    }

    @Test(expected = NotFoundException.class)
    public void patch_on_a_non_existing_resource_should_return_404() throws Exception {
        entityResource.patch(WORK, "a_missing_work1234", "{}");
    }

    @Test(expected=NotFoundException.class)
    public void delete__non_existing_work_should_raise_not_found_exception(){
        entityResource.delete(WORK, "work_DOES_NOT_EXIST_AND_FAILS");
    }

    @Test
    public void delete_existing_work_should_return_no_content() throws URISyntaxException{
        String work = createTestRDF(SOME_WORK_IDENTIFIER, WORK);
        Response createResponse = entityResource.createFromLDJSON(WORK, work);
        String workId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/" + WORK + "/", "");
        Response response = entityResource.delete(WORK, workId);
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
    }

    @Test
    public void create_should_return_201_when_publication_created() throws Exception{
        when(mockKohaAdapter.getNewBiblioWithMarcRecord(any())).thenReturn(A_BIBLIO_ID);
        Response result = entityResource.createFromLDJSON(PUBLICATION, createTestRDF("publication_SHOULD_EXIST", PUBLICATION));
        assertNull(result.getEntity());
        assertEquals(CREATED.getStatusCode(), result.getStatus());
    }

    @Test
    public void location_returned_from_create_should_return_the_new_publication() throws Exception{
        when(mockKohaAdapter.getNewBiblioWithMarcRecord(new MarcRecord())).thenReturn(A_BIBLIO_ID);
        Response createResponse = entityResource.createFromLDJSON(PUBLICATION, createTestRDF("publication_SHOULD_EXIST", PUBLICATION));

        String publicationId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/publication/", "");

        Response result = entityResource.get(PUBLICATION, publicationId);

        assertNotNull(result);
        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());
        assertEquals(OK.getStatusCode(), result.getStatus());
        assertTrue(result.getEntity().toString().contains("\"deichman:recordID\""));
        assertValidJSON(result.getEntity().toString());
    }

    @Test
    public void delete_publication_should_return_no_content() throws Exception{
        when(mockKohaAdapter.getNewBiblioWithMarcRecord(new MarcRecord())).thenReturn(A_BIBLIO_ID);
        Response createResponse = entityResource.createFromLDJSON(PUBLICATION, createTestRDF("publication_SHOULD_BE_PATCHABLE", PUBLICATION));
        String publicationId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/publication/", "");
        Response response = entityResource.delete(PUBLICATION, publicationId);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void patch_should_actually_persist_changes() throws Exception {
        when(mockKohaAdapter.getNewBiblioWithMarcRecord(new MarcRecord())).thenReturn(A_BIBLIO_ID);
        String publication = createTestRDF("publication_SHOULD_BE_PATCHABLE", PUBLICATION);
        Response result = entityResource.createFromLDJSON(PUBLICATION, publication);
        String publicationId = result.getLocation().getPath().substring("/publication/".length());
        String patchData = "{"
                + "\"op\": \"add\","
                + "\"s\": \"" + result.getLocation().toString() + "\","
                + "\"p\": \"http://deichman.no/ontology#color\","
                + "\"o\": {"
                + "\"value\": \"red\""
                + "}"
                + "}";
        Response patchResponse = entityResource.patch(PUBLICATION, publicationId, patchData);
        Model testModel = ModelFactory.createDefaultModel();
        String response = patchResponse.getEntity().toString();
        InputStream in = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(testModel, in, Lang.JSONLD);
        Statement s = ResourceFactory.createStatement(
                ResourceFactory.createResource(result.getLocation().toString()),
                ResourceFactory.createProperty("http://deichman.no/ontology#color"),
                ResourceFactory.createPlainLiteral("red"));
        assertTrue(testModel.contains(s));
    }

    @Test
    public void work_should_have_language_labels() throws URISyntaxException {
        String work = "{\n"
                + "    \"@context\": {\n"
                + "        \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/publication/work_should_have_language_labels\",\n"
                + "        \"@type\": \"deichman:Work\"\n,"
                + "        \"deichman:language\": \"http://lexvo.org/id/iso639-3/eng\"\n"
                + "    }\n"
                + "}";
        Response createResponse = entityResource.createFromLDJSON(WORK, work);
        String workId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/work/", "");
        Response result = entityResource.get(WORK, workId);

        String labelsComparison = "{\n"
                + "    \"@id\" : \"http://lexvo.org/id/iso639-3/eng\",\n"
                + "    \"@type\" : \"http://lexvo.org/ontology#Language\",\n"
                + "    \"rdfs:label\" : {\n"
                + "      \"@language\" : \"no\",\n"
                + "      \"@value\" : \"Engelsk\"\n"
                + "    }\n"
                + "  }";

        assertEquals(OK.getStatusCode(), result.getStatus());
        assertTrue(result.getEntity().toString().contains(labelsComparison));
    }

    @Test
    public void work_should_have_format_labels() throws URISyntaxException {
        String work = "{\n"
                + "    \"@context\": {\n"
                + "        \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/publication/work_should_have_language_labels\",\n"
                + "        \"@type\": \"deichman:Work\"\n,"
                + "        \"deichman:format\": \"http://data.deichman.no/format#Book\"\n"
                + "    }\n"
                + "}";
        Response createResponse = entityResource.createFromLDJSON(WORK, work);
        String workId = createResponse.getHeaderString(LOCATION).replaceAll("http://deichman.no/work/", "");
        Response result = entityResource.get(WORK, workId);

        String labelsComparison = "{\n"
                + "    \"@id\" : \"http://data.deichman.no/format#Book\",\n"
                + "    \"@type\" : \"http://data.deichman.no/utility#Format\",\n"
                + "    \"rdfs:label\" : {\n"
                + "      \"@language\" : \"no\",\n"
                + "      \"@value\" : \"Bok\"\n"
                + "    }\n"
                + "  }";
        assertEquals(OK.getStatusCode(), result.getStatus());
        assertTrue(result.getEntity().toString().contains(labelsComparison));
    }

    @Test
    public void should_return_created_response_from_ntriples_input() throws URISyntaxException {
        String ntriples = "<#> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://deichman.no/ontology#Work> .";
        Response createResponse = entityResource.createFromNTriples("work", ntriples);
        assertEquals(CREATED.getStatusCode(), createResponse.getStatus());
    }

    @Test
    public void should_return_location_from_ntriples_input() throws URISyntaxException {
        String ntriples = "<#> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://deichman.no/ontology#Work> .";
        Response createResponse = entityResource.createFromNTriples("work", ntriples);
        assertTrue(createResponse.getLocation().toString().contains("http://"));
    }

    private String createTestRDF(String identifier, String type) {
        String ontologyClass = WordUtils.capitalize(type);
        return "{\n"
                + "    \"@context\": {\n"
                + "        \"dcterms\": \"http://purl.org/dc/terms/\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/" + type + "/" + identifier + "\",\n"
                + "        \"@type\": \"deichman:" + ontologyClass + "\",\n"
                + "        \"dcterms:identifier\": \"" + identifier + "\"\n"
                + "    }\n"
                + "}";
    }

    private String createPersonImportRDF(String identifier, String type, String personId) {
        String ontologyClass = WordUtils.capitalize(type);
        return "{\n"
                + "    \"@context\": {\n"
                + "        \"dcterms\": \"http://purl.org/dc/terms/\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/" + type + "/" + identifier + "\",\n"
                + "        \"@type\": \"deichman:" + ontologyClass + "\",\n"
                + "        \"http://data.deichman.no/duo#bibliofilPersonId\": \"" + personId + "\"\n"
                + "    }\n"
                + "}";
    }

    private String createPlaceOfPublicationImportRDF(String identifier, String type, String placeId) {
        String ontologyClass = WordUtils.capitalize(type);
        return "{\n"
                + "    \"@context\": {\n"
                + "        \"dcterms\": \"http://purl.org/dc/terms/\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/" + type + "/" + identifier + "\",\n"
                + "        \"@type\": \"deichman:" + ontologyClass + "\",\n"
                + "        \"http://data.deichman.no/duo#bibliofilPlaceOfPublicationId\": \"" + placeId + "\"\n"
                + "    }\n"
                + "}";
    }


}
