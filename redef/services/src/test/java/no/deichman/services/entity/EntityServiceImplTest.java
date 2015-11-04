package no.deichman.services.entity;

import no.deichman.services.entity.kohaadapter.KohaAdapter;
import no.deichman.services.entity.kohaadapter.Marc2Rdf;
import no.deichman.services.entity.kohaadapter.MarcConstants;
import no.deichman.services.entity.patch.PatchParserException;
import no.deichman.services.entity.repository.InMemoryRepository;
import no.deichman.services.uridefaults.BaseURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static no.deichman.services.entity.EntityType.PERSON;
import static no.deichman.services.entity.EntityType.PUBLICATION;
import static no.deichman.services.entity.EntityType.WORK;
import static no.deichman.services.entity.repository.InMemoryRepositoryTest.repositoryWithDataFrom;
import static no.deichman.services.rdf.RDFModelUtil.modelFrom;
import static no.deichman.services.rdf.RDFModelUtil.stringFrom;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.jena.rdf.model.ResourceFactory.createLangLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@RunWith(MockitoJUnitRunner.class)
public class EntityServiceImplTest {

    private static final String A_BIBLIO_ID = "234567";
    public static final String SOME_BARCODE = "0123456789";
    private EntityServiceImpl service;
    private InMemoryRepository repository;
    private String ontologyURI;
    private String workURI;
    private String publicationURI;
    private String personURI;


    @Mock
    private KohaAdapter mockKohaAdapter;

    @Before
    public void setup(){
        BaseURI baseURI = BaseURI.local();
        repository = new InMemoryRepository();
        service = new EntityServiceImpl(baseURI, repository, mockKohaAdapter);
        ontologyURI = baseURI.ontology();
        workURI = baseURI.work();
        publicationURI = baseURI.publication();
        personURI = baseURI.person();
    }

    @Test
    public void test_retrieve_work_by_id(){
        String testId = "work_SHOULD_EXIST";
        String workData = getTestJSON(testId, "work");
        Model inputModel = modelFrom(workData, JSONLD);
        String workId = service.create(WORK, inputModel);
        Model comparison = ModelFactory.createDefaultModel();
        InputStream in = new ByteArrayInputStream(
                workData.replace(workURI + testId, workId)
                .getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(comparison, in, JSONLD);
        Model test = service.retrieveById(WORK, workId.replace(workURI, ""));
        assertTrue(test.isIsomorphicWith(comparison));
    }

    @Test
    public void test_retrieve_publication_by_id() throws Exception{
        when(mockKohaAdapter.getNewBiblio()).thenReturn(A_BIBLIO_ID);
        String testId = "publication_SHOULD_EXIST";
        String publicationData = getTestJSON(testId, "publication");
        Model inputModel = modelFrom(publicationData, JSONLD);
        String publicationId = service.create(PUBLICATION, inputModel);
        Model test = service.retrieveById(PUBLICATION, publicationId.replace(publicationURI, ""));
        Statement testStatement = createStatement(
                createResource(publicationId),
                createProperty(RDF.type.getURI()),
                createResource(ontologyURI + "Publication"));
        assertTrue(test.contains(testStatement));
    }

    @Test
    public void test_retrieve_work_by_id_with_language() {
        String testId = "test_retrieve_work_by_id_with_language";
        String workData = "{\n"
                + "    \"@context\": {\n"
                + "        \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/publication/" + testId + "\",\n"
                + "        \"@type\": \"deichman:Work\"\n,"
                + "        \"deichman:language\": \"http://lexvo.org/id/iso639-3/eng\"\n"
                + "    }\n"
                + "}";
        Model inputModel = modelFrom(workData, JSONLD);
        String workId = service.create(WORK, inputModel);
        Model test = service.retrieveById(WORK, workId.replace(workURI, ""));
        assertTrue(
                test.contains(
                        createStatement(
                                createResource("http://lexvo.org/id/iso639-3/eng"),
                                createProperty(RDFS.label.getURI()),
                                createLangLiteral("Engelsk", "no")
                        )
                )
        );
    }

    static Model modelForBiblio() { // TODO return much simpler model
        Model model = ModelFactory.createDefaultModel();
        Model m = ModelFactory.createDefaultModel();
        InputStream in = EntityServiceImplTest.class.getClassLoader().getResourceAsStream("marc.xml");
        MarcReader reader = new MarcXmlReader(in);
        Marc2Rdf marcRdf = new Marc2Rdf(BaseURI.local());
        while (reader.hasNext()) {
            Record record = reader.next();
            m.add(marcRdf.mapItemsToModel(record.getVariableFields(MarcConstants.FIELD_952)));
        }
        model.add(m);
        return model;
    }

    @Test
    public void test_retrieve_work_by_id_with_format() {
        String testId = "test_retrieve_work_by_id_with_format";
        String workData = "{\n"
                + "    \"@context\": {\n"
                + "        \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n"
                + "        \"deichman\": \"http://deichman.no/ontology#\"\n"
                + "    },\n"
                + "    \"@graph\": {\n"
                + "        \"@id\": \"http://deichman.no/publication/" + testId + "\",\n"
                + "        \"@type\": \"deichman:Work\"\n,"
                + "        \"deichman:format\": \"http://data.deichman.no/format#Book\"\n"
                + "    }\n"
                + "}";
        Model inputModel = modelFrom(workData, JSONLD);
        String workId = service.create(WORK, inputModel);
        Model test = service.retrieveById(WORK, workId.replace(workURI, ""));
        assertTrue(
                test.contains(
                        createStatement(
                                createResource("http://data.deichman.no/format#Book"),
                                createProperty(RDFS.label.getURI()),
                                createLangLiteral("Bok", "no")
                        )
                )
        );
    }

    @Test
    public void test_retrieve_work_items_by_id(){
        when(mockKohaAdapter.getBiblio("626460")).thenReturn(EntityServiceImplTest.modelForBiblio());
        EntityService myService = new EntityServiceImpl(BaseURI.local(), repositoryWithDataFrom("testdata.ttl"), mockKohaAdapter);

        Model m = myService.retrieveWorkItemsById("work_TEST_KOHA_ITEMS_LINK");
        Property p = createProperty("http://deichman.no/ontology#editionOf");
        ResIterator ni = m.listSubjectsWithProperty(p);

        int i = 0;
        while (ni.hasNext()) {
            i++;
            ni.next();
        }
        final int expectedNoOfItems = 34;
        assertEquals(expectedNoOfItems, i);
    }

    @Test
    public void test_create_work(){
        String testId = "SERVICE_WORK_SHOULD_EXIST";
        String work = getTestJSON(testId, "work");
        Model inputModel = modelFrom(work, JSONLD);

        final String uri = service.create(WORK, inputModel);

        Statement s = createStatement(
                createResource(uri),
                createProperty(DCTerms.identifier.getURI()),
                createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
    }

    @Test
    public void test_create_publication(){
        when(mockKohaAdapter.getNewBiblio()).thenReturn(A_BIBLIO_ID);
        String testId = "SERVICE_PUBLICATION_SHOULD_EXIST";
        String publication = getTestJSON(testId, "publication");
        Model inputModel = modelFrom(publication, JSONLD);

        String publicationId = service.create(PUBLICATION, inputModel);

        Statement s = createStatement(
                createResource(publicationId),
                createProperty(DCTerms.identifier.getURI()),
                createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
    }

    @Test
    public void test_create_person(){
        String testId = "SERVICE_PERSON_SHOULD_EXIST";
        String person = getTestJSON(testId, "person");
        Model inputModel = modelFrom(person, JSONLD);

        final String uri = service.create(PERSON, inputModel);

        Statement s = createStatement(
                createResource(uri),
                createProperty(DCTerms.identifier.getURI()),
                createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
    }

    @Test
    public void test_create_publication_with_items() {
        List<Map<Character, String>> list = new ArrayList<Map<Character, String>>();
        Map<Character, String> arr = new HashMap<Character, String>(){{
            put('a', "hutl");
            put('b', "hutl");
            put('c', "m");
            put('l', "3");
            put('m', "1");
            put('o', "952 Cri");
            put('p', "213123123");
            put('q', "2014-11-05");
            put('t', "1");
            put('y', "L");
        }};

        when(mockKohaAdapter.getNewBiblioWithItems(arr)).thenReturn("123");
        Model test = modelFrom(itemNTriples("213123123").replaceAll("__BASEURI__", "http://deichman.no/"), Lang.NTRIPLES);
        String response = service.create(PUBLICATION, test);
        assertTrue(response.substring(response.lastIndexOf("/") + 1, response.length()).matches("p[0-9]+"));
    }

    @Test
    public void test_create_and_delete_entity() throws Exception{
        when(mockKohaAdapter.getNewBiblio()).thenReturn(A_BIBLIO_ID);
        String testId = "publication_SHOULD_BE_DELETED";
        String publication = getTestJSON(testId, "publication");
        Model inputModel = modelFrom(publication, JSONLD);
        String publicationId = service.create(PUBLICATION, inputModel);
        Statement s = createStatement(
                createResource(publicationId),
                createProperty(DCTerms.identifier.getURI()),
                createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));

        Model test = ModelFactory.createDefaultModel();
        InputStream in = new ByteArrayInputStream(
                publication.replace(publicationURI + testId, publicationId)
                .getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(test,in, JSONLD);
        service.delete(test);
        assertFalse(repository.askIfStatementExists(s));
    }

    @Test
    public void test_patch_work_add() throws Exception{
        String testId = "work_SHOULD_BE_PATCHABLE";
        String workData = getTestJSON(testId, "work");
        Model inputModel = modelFrom(workData, JSONLD);
        String workId = service.create(WORK, inputModel);

        Model oldModel = ModelFactory.createDefaultModel();

        String comparisonRDF = workData.replace(workURI + testId, workId);
        InputStream oldIn = new ByteArrayInputStream(comparisonRDF.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(oldModel, oldIn, JSONLD);
        String nonUriWorkId = workId.replace(workURI, "");
        Model data = service.retrieveById(WORK, nonUriWorkId);
        assertTrue(oldModel.isIsomorphicWith(data));
        String patchData = getTestPatch("add", workId);
        Model patchedModel = service.patch(WORK, nonUriWorkId, patchData);
        assertTrue(patchedModel.contains(
                createResource(workId),
                createProperty(ontologyURI + "color"),
                "red"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFDataMgr.write(baos,patchedModel.difference(oldModel),Lang.NT);
        assertEquals(baos.toString("UTF-8").trim(),
                "<"+ workId + "> <" + ontologyURI + "color> \"red\" .");
    }

    @Test
    public void test_patch_publication_add() throws Exception{
        when(mockKohaAdapter.getNewBiblio()).thenReturn(A_BIBLIO_ID);
        String testId = "publication_SHOULD_BE_PATCHABLE";
        String publicationData = getTestJSON(testId,"publication");
        Model inputModel = modelFrom(publicationData, JSONLD);
        String publicationId = service.create(PUBLICATION, inputModel);
        String nonUriPublicationId = publicationId.replace(publicationURI, "");
        String patchData = getTestPatch("add", publicationId);
        Model patchedModel = service.patch(PUBLICATION, nonUriPublicationId, patchData);
        assertTrue(patchedModel.contains(
                createResource(publicationId),
                createProperty(ontologyURI + "color"),
                "red"));
    }

    @Test(expected=PatchParserException.class)
    public void test_bad_patch_fails() throws Exception{
        String workData = getTestJSON("SHOULD_FAIL","work");
        Model inputModel = modelFrom(workData, JSONLD);
        String workId = service.create(WORK, inputModel);
        String badPatchData = "{\"po\":\"cas\",\"s\":\"http://example.com/a\"}";
        service.patch(WORK, workId.replace(workURI, ""), badPatchData);
    }

    @Test
    public void test_retrieve_work_by_creator() throws Exception {
        String testId = "PERSON_THAT_SHOULD_HAVE_CREATED_WORK";
        String person = getTestJSON(testId, "person");
        Model personModel = modelFrom(person, JSONLD);

        final String personUri = service.create(PERSON, personModel);

        String work = getTestJSON("workId", "work");
        Model workModel = modelFrom(work, JSONLD);
        final String workUri = service.create(WORK, workModel);

        String work2 = getTestJSON("workId2", "work");
        Model workModel2 = modelFrom(work2, JSONLD);
        String workUri2 = service.create(WORK, workModel2);

        addCreatorToWorkPatch(personUri, workUri);
        addCreatorToWorkPatch(personUri, workUri2);
        Model worksByCreator = service.retrieveWorksByCreator(personUri.substring(personUri.lastIndexOf("/h")+1));
        assertFalse(worksByCreator.isEmpty());
        assertThat(stringFrom(worksByCreator, JSONLD),
                sameJSONAs(format(""
                        + "{"
                        + "  \"@graph\":["
                        + "      {"
                        + "        \"@id\":\"%s\","
                        + "        \"rdfs:name\": \"Work Name\""
                        + "      },"
                        + "      {"
                        + "        \"@id\":\"%s\","
                        + "        \"rdfs:name\": \"Work Name\""
                        + "      }"
                        + "    ]"
                        + "}", workUri, workUri2))
                        .allowingExtraUnexpectedFields()
                        .allowingAnyArrayOrdering());
    }

    private void addCreatorToWorkPatch(String personUri, String workUri) throws PatchParserException {
        service.patch(WORK, workUri, format(""
                + "{"
                + "  \"op\":\"add\",\n"
                + "  \"s\": \"%s\",\n"
                + "  \"p\":\"%screator\",\n"
                + "  \"o\":{\n"
                + "    \"value\":\"%s\",\n"
                + "    \"type\": \"%s\"\n"
                + "   }\n"
                + "}\n", workUri, ontologyURI, personUri, XSD.anyURI.getURI()));
    }

    private String getTestJSON(String id, String type) {
        String resourceClass = null;
        String resourceURI = null;

        switch (type.toLowerCase()) {
            case "work" : resourceClass = "Work";
                resourceURI = workURI;
                break;
            case "publication" : resourceClass = "Publication";
                resourceURI = publicationURI;
                break;
            case "person" : resourceClass = "Person";
                resourceURI = personURI;
                break;
            default: break;
        }

        return format("{\"@context\": "
                + "{\"dcterms\": \"http://purl.org/dc/terms/\","
                + "\"deichman\": \"" + ontologyURI + "\","
                + "\"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\"},"
                + "\"@graph\": "
                + "{\"@id\": \"" + resourceURI + "" + id + "\","
                + "\"@type\": \"deichman:" + resourceClass + "\","
                + "\"rdfs:name\": \"%s Name\","
                + "\"dcterms:identifier\":\"" + id + "\"}}", capitalize(type));
    }

    private String getTestPatch(String operation, String id) {
        return "{"
                + "\"op\": \"" + operation + "\","
                + "\"s\": \"" + id + "\","
                + "\"p\": \"" + ontologyURI + "color\","
                + "\"o\": {"
                + "\"value\": \"red\""
                + "}"
                + "}";
    }

    private String itemNTriples(final String barcode) {
        return "<__BASEURI__bibliofilResource/1527411> <" + ontologyURI + "hasItem> <__BASEURI__bibliofilItem/x" + barcode + "> .\n"
                + "<__BASEURI__bibliofilResource/1527411> <" + ontologyURI + "name> \"Titlely title\".\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/a> \"hutl\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/b> \"hutl\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/c> \"m\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/l> \"3\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/m> \"1\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/o> \"952 Cri\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/p> \"" + barcode + "\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/q> \"2014-11-05\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/t> \"1\" .\n"
                + "<__BASEURI__bibliofilItem/x" + barcode + "> <" + ontologyURI + "itemSubfieldCode/y> \"L\" .";
    }
}
