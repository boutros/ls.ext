package no.deichman.services.rest.utils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.util.Map;
import static no.deichman.services.rdf.RDFModelUtil.modelFrom;
import no.deichman.services.uridefaults.BaseURI;
import no.deichman.services.uridefaults.BaseURIMock;
import org.apache.jena.riot.Lang;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class JSONLDCreatorTest {

    @Test
    public void simple_representation_should_be_isomorphic_after_conversion() {
        JSONLDCreator jsonldCreator = new JSONLDCreator(new BaseURIMock());

        String inputAsN3 = "<http://example.com/test> <http://example.com/onto/resource> <http://example.com/r/m2> .";
        Model modelBeforeJsonLDConversion = modelFrom(inputAsN3, Lang.NTRIPLES);

        String jsonLD = jsonldCreator.asJSONLD(modelBeforeJsonLDConversion);
        Model modelAfterJsonLDConversion = modelFrom(jsonLD, Lang.JSONLD);

        assertTrue(modelBeforeJsonLDConversion.isIsomorphicWith(modelAfterJsonLDConversion));
    }

    @Test
    public void JSONLD_contains_our_special_contexts() {
        JSONLDCreator jsonldCreator = new JSONLDCreator();
        BaseURI baseURI = new BaseURIMock();
        jsonldCreator.setBaseURI(baseURI);

        String inputAsN3 = "<http://example.com/test> <http://example.com/onto/resource> <http://example.com/r/m2> .";

        String jsonld = jsonldCreator.asJSONLD(modelFrom(inputAsN3, Lang.NTRIPLES));
        Model model = modelFrom(jsonld, Lang.JSONLD);

        Map<String, String> ns = model.getNsPrefixMap();
        assertTrue(ns.get("deichman").equals(baseURI.getOntologyURI()));
        assertTrue(ns.get("rdfs").equals(RDFS.getURI()));
    }

}