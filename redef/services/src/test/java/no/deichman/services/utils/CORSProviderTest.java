package no.deichman.services.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response;

import org.junit.Test;

public class CORSProviderTest {

    @Test
    public void test_it_exists(){
        assertNotNull(new CORSProvider());
    }

    @Test
    public void test_it_can_make_CORS_response(){
        CORSProvider cors = new CORSProvider();
        String reqHeader = "";
        Response response = cors.makeCORSResponse(Response.ok(),reqHeader);
        assertNotNull(response);
        assert(response.getHeaderString("Access-Control-Allow-Headers") == null);
        assertEquals(response.getHeaderString("Access-Control-Allow-Origin"),"*");
        assertEquals(response.getHeaderString("Access-Control-Allow-Methods"),"GET, POST, OPTIONS, PUT, PATCH");
    }
}
