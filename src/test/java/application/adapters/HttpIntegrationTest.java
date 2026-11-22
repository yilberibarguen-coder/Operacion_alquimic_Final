package application.adapters;

import application.domain.ports.out.ReadingRepositoryPort;
import application.domain.valueobjects.SurveillanceDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired ReadingRepositoryPort repository;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final String LABS = "{\"deviceId\":\"STAR_LABS\",\"distance\":670.82,\"message\":[\"hola\",\"\"]}";
    private static final String CCPD = "{\"deviceId\":\"CCPD\",\"distance\":200,\"message\":[\"\",\"mundo\"]}";
    private static final String SATELLITE = "{\"deviceId\":\"STAR_SATELLITE\",\"distance\":400,\"message\":[\"\",\"\"]}";

    @BeforeEach void reset() {
        for (var device : SurveillanceDevice.values()) repository.deleteById(device);
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return mapper.readTree(response.body());
    }

    private void assertError(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        var error = json(response);
        assertEquals(status, error.get("status").asInt());
        assertFalse(error.get("message").asString().isBlank());
        assertNotNull(error.get("timestamp"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"devices\":[null,null,null]}", "{\"devices\":[null]}",
            "{\"devices\":[]}", "{\"devices\":null}", "{}", "{", "{\"devices\":[{}]}"})
    void invalidCompleteRequestsReturn400InsteadOf500(String body) throws Exception {
        assertError(request("POST", "/analysis", body), 400);
        assertTrue(repository.findAll().isEmpty());
    }

    @Test void completeAnalysisReturnsBothResultsWithoutRegistering() throws Exception {
        var response = request("POST", "/analysis", "{\"devices\":[" + SATELLITE + "," + LABS + "," + CCPD + "]}");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(100.0, json(response).get("position").get("x").asDouble(), 0.1);
        assertEquals(100.0, json(response).get("position").get("y").asDouble(), 0.1);
        assertEquals("hola mundo", json(response).get("message").asString());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test void impossibleCompleteDistancesReturn422() throws Exception {
        String body = "{\"devices\":[" + LABS.replace("670.82", "1") + ","
                + CCPD.replace("200", "1") + "," + SATELLITE.replace("400", "1") + "]}";
        assertError(request("POST", "/analysis", body), 422);
        assertTrue(repository.findAll().isEmpty());
    }

    @Test void registerConsultReplacePatchAndDeletePreserveOtherDevices() throws Exception {
        assertEquals(201, request("POST", "/devices", SATELLITE).statusCode());
        assertEquals(201, request("POST", "/devices", LABS).statusCode());
        assertError(request("GET", "/analysis", null), 422);
        assertFalse(json(request("GET", "/devices", null)).get("sufficientData").asBoolean());
        assertEquals(201, request("POST", "/devices", CCPD).statusCode());
        assertEquals("hola mundo", json(request("GET", "/analysis", null)).get("message").asString());
        assertError(request("POST", "/devices", CCPD), 409);

        var originalLabs = json(request("GET", "/devices/STAR_LABS", null));
        var originalSatellite = json(request("GET", "/devices/STAR_SATELLITE", null));
        assertError(request("PUT", "/devices/CCPD", "{\"distance\":300}"), 400);
        assertEquals(200, request("PUT", "/devices/CCPD", "{\"distance\":300,\"message\":[\"\",\"equipo\"]}").statusCode());
        assertFalse(json(request("GET", "/devices", null)).get("sufficientData").asBoolean());
        assertError(request("GET", "/analysis", null), 422);
        var updated = request("PATCH", "/devices/CCPD", "{\"distance\":200}");
        assertEquals(200, updated.statusCode());
        assertEquals("equipo", json(updated).get("message").get(1).asString());
        updated = request("PATCH", "/devices/CCPD", "{\"message\":[\"\",\"mundo\"]}");
        assertEquals(200.0, json(updated).get("distance").asDouble());
        assertError(request("PATCH", "/devices/CCPD", "{}"), 400);
        assertError(request("PATCH", "/devices/CCPD", "{\"distance\":-1}"), 400);
        assertEquals("hola mundo", json(request("GET", "/analysis", null)).get("message").asString());
        assertEquals(204, request("DELETE", "/devices/CCPD", null).statusCode());
        assertError(request("GET", "/devices/CCPD", null), 404);
        assertError(request("GET", "/analysis", null), 422);
        assertEquals(originalLabs, json(request("GET", "/devices/STAR_LABS", null)));
        assertEquals(originalSatellite, json(request("GET", "/devices/STAR_SATELLITE", null)));
        assertError(request("GET", "/devices/UNKNOWN", null), 400);
    }
}
