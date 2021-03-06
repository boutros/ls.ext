package no.deichman.services.search;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;

import static no.deichman.services.search.ModelToIndexMapper.ModelToIndexMapperBuilder.modelToIndexMapperBuilder;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

/**
 * Responsibility: unit test ModelToIndexMapper.
 */
public class ModelToIndexMapperTest {
    public static final String GROUP_CONCATENATOR = "|";
    public static final String QUERY = ""
            + "select ?person ?name ?nationalityLabel ?work "
            + "      (group_concat(concat(?workTitle, \"@\", lang(?workTitle));separator=\"" + GROUP_CONCATENATOR + "\") as ?workTitles) ?workYear\n"
            + "where { \n"
            + "    ?person a <http://deichman.no/ontology#person> ; \n"
            + "              <http://deichman.no/name> ?name ; \n"
            + "              <http://deichman.no/nationality> ?nationality . \n"
            + "     ?nationality a <http://data.deichman.no/utility#Nationality> ;\n"
            + "                  <http://www.w3.org/2000/01/rdf-schema#label> ?nationalityLabel .\n"
            + "     optional {?work a <http://deichman.no/ontology#work> ;"
            + "                       <http://deichman.no/ontology#creator> ?person ;\n"
            + "                       <http://deichman.no/ontology#mainTitle> ?workTitle ; \n"
            + "      }\n"
            + "}"
            + "group by ?person ?name ?nationalityLabel ?work ?workYear\n";

    @Test
    public void testModelToIndexDocument() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/person_1"),
                RDF.type,
                ResourceFactory.createResource("http://deichman.no/ontology#person")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/person_1"),
                ResourceFactory.createProperty("http://deichman.no/a_prop"),
                ResourceFactory.createPlainLiteral("a_prop_value")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/person_1"),
                ResourceFactory.createProperty("http://deichman.no/name"),
                ResourceFactory.createPlainLiteral("personName_value")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/person_1"),
                ResourceFactory.createProperty("http://deichman.no/nationality"),
                ResourceFactory.createResource("http://deichman.no/nationality#n")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/nationality#n"),
                ResourceFactory.createProperty("http://deichman.no/nationality"),
                ResourceFactory.createResource("http://deichman.no/nationality#n")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/nationality#n"),
                RDF.type,
                ResourceFactory.createResource("http://data.deichman.no/utility#Nationality")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/nationality#n"),
                ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"),
                ResourceFactory.createLangLiteral("Norsk", "no")));

                model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/person_2"),
                ResourceFactory.createProperty("http://deichman.no/a_prop"),
                ResourceFactory.createPlainLiteral("a_prop_value_2")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_1"),
                RDF.type,
                ResourceFactory.createResource("http://deichman.no/ontology#work")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_1"),
                ResourceFactory.createProperty("http://deichman.no/ontology#creator"),
                ResourceFactory.createResource("http://deichman.no/person_1")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_1"),
                ResourceFactory.createProperty("http://deichman.no/ontology#mainTitle"),
                ResourceFactory.createPlainLiteral("work_1_title")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_2"),
                RDF.type,
                ResourceFactory.createResource("http://deichman.no/ontology#work")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_2"),
                ResourceFactory.createProperty("http://deichman.no/ontology#creator"),
                ResourceFactory.createResource("http://deichman.no/person_1")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_2"),
                ResourceFactory.createProperty("http://deichman.no/ontology#mainTitle"),
                ResourceFactory.createPlainLiteral("work_2_title")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://deichman.no/work_2"),
                ResourceFactory.createProperty("http://deichman.no/ontology#mainTitle"),
                ResourceFactory.createLangLiteral("work_2_english_title", "en")));

        Pair<String, String> uriAndDocument = modelToIndexMapperBuilder().targetIndexType("person")
                .selectQuery(QUERY)
                .mapFromResultVar("person").toJsonPath("person.uri")
                .mapFromResultVar("name").toJsonPath("person.name")
                .mapFromResultVar("nationalityLabel").toJsonPath("person.nationality")
                .mapFromResultVar("work").toJsonObjectArray("person.work").withObjectMember("uri")
                .mapFromResultVar("workTitles").toJsonObjectArray("person.work").withLanguageSpecifiedObjectMember("mainTitle")
                .mapFromResultVar("workYear").toJsonObjectArray("person.work").withObjectMember("publicationYear")
                .arrayGroupBy("work")
                .withGroupConcatenator(GROUP_CONCATENATOR)
                .withOntologyPrefix("http://deichman.no/ontology#")
                .build()
                .modelToIndexDocument(model)
                .get();

        Assert.assertEquals("http://deichman.no/person_1", uriAndDocument.getKey());

        String jsonDocument = uriAndDocument.getValue();
        Assert.assertThat(jsonDocument, sameJSONAs(""
                + "{"
                + "  \"person\": {"
                + "      \"uri\": \"http://deichman.no/person_1\","
                + "      \"name\": \"personName_value\","
                + "      \"nationality\": \"Norsk\","
                + "      \"work\": ["
                + "           {"
                + "               \"uri\": \"http://deichman.no/work_1\","
                + "               \"mainTitle\": {"
                + "                    \"default\": \"work_1_title\""
                + "                }"
                + "           },"
                + "           {"
                + "               \"uri\": \"http://deichman.no/work_2\","
                + "               \"mainTitle\": {"
                + "                    \"default\": \"work_2_title\", "
                + "                    \"en\": \"work_2_english_title\" "
                + "                 }"
                + "           }"
                + "       ]"
                + "  }"
                + "}").allowingAnyArrayOrdering());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalJsonPath() {
        modelToIndexMapperBuilder().mapFromResultVar("var").toJsonPath("path#.path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingResultVar() {
        modelToIndexMapperBuilder().toJsonPath("path.path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingQuery() {
        modelToIndexMapperBuilder().targetIndexType("type").mapFromResultVar("var").toJsonPath("path.path").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingIndexType() {
        modelToIndexMapperBuilder().selectQuery("SELECT ...").mapFromResultVar("var").toJsonPath("path.path").build();
    }
}
