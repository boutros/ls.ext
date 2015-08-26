package no.deichman.services.rest.utils;

import java.io.IOException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import static org.junit.Assert.assertTrue;

/**
 * Responsibility: JSON test tool.
 */
public final class TestJSON {

    private TestJSON() { }

    public static void assertValidJSON(String string) {
        assertTrue(isValidJSON(string));
    }

    private static boolean isValidJSON(final String json) {
        boolean valid = false;
        try {
            final JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(json);
            while (parser.nextToken() != null) {
                // NOOP
            }
            valid = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return valid;
    }

}
