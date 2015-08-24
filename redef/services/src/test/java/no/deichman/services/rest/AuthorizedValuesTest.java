package no.deichman.services.rest;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import no.deichman.services.rdf.RDFModelUtil;
import org.apache.jena.riot.Lang;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AuthorizedValuesTest {

    private AuthorizedValues authorizedValues;

    @Before
    public void setUp() throws Exception {
        authorizedValues = new AuthorizedValues();
    }

    @Test
    public void should_have_default_constructor() {
        assertNotNull(new AuthorizedValues());
    }

    @Test
    public void should_return_ok_when_getting_languages() throws Exception {

        Response response = authorizedValues.language();
        assertThat(response.getStatus(), equalTo(Status.OK.getStatusCode()));
    }

    @Test
    public void should_actually_return_some_language_data() throws Exception {

        Object body = authorizedValues.language().getEntity();
        Model model = RDFModelUtil.modelFrom((String) body, Lang.JSONLD);
        boolean hasEnglish = model.contains(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://lexvo.org/id/iso639-3/eng"),
                RDFS.label,
                ResourceFactory.createLangLiteral("Engelsk", "no")
        ));
        assertTrue("model doesn't have English", hasEnglish);
    }
}