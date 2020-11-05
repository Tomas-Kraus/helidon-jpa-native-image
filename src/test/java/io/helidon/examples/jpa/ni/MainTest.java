
package io.helidon.examples.jpa.ni;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.CDI;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

class MainTest {

    private static Server server;
    private static String serverUrl;

    @BeforeAll
    public static void startTheServer() throws Exception {
        server = Server.create().start();
        serverUrl = "http://localhost:" + server.port();
    }

    @Test
    @Order(1)
    void testAddPerson() {
        Client client = ClientBuilder.newClient();

        JsonObject jsonObject = Json.createObjectBuilder()
                .add("nick", "joe")
                .add("name", "Joe Brown")
                .build();
        Response r = client
                .target(serverUrl)
                .path("greet")
                .request()
                .post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
        Assertions.assertEquals(200, r.getStatus(), "POST person status code");

        jsonObject = Json.createObjectBuilder()
                .add("nick", "jose")
                .add("name", "Jose Carreras")
                .build();
        r = client
                .target(serverUrl)
                .path("greet")
                .request()
                .post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
        Assertions.assertEquals(200, r.getStatus(), "POST person status code");
    }

    @Test
    @Order(1)
    void testHelloWorld() {
        Client client = ClientBuilder.newClient();

        JsonObject jsonObject = client
                .target(serverUrl)
                .path("greet")
                .request()
                .get(JsonObject.class);
        Assertions.assertEquals("Hello World!", jsonObject.getString("message"),
                "default message");

        jsonObject = client
                .target(serverUrl)
                .path("greet/joe")
                .request()
                .get(JsonObject.class);
        Assertions.assertEquals("Hello Joe Brown!", jsonObject.getString("message"),
                "hello Joe message");

        Response r = client
                .target(serverUrl)
                .path("greet/greeting")
                .request()
                .put(Entity.entity("{\"greeting\" : \"Hola\"}", MediaType.APPLICATION_JSON));
        Assertions.assertEquals(204, r.getStatus(), "PUT status code");

        jsonObject = client
                .target(serverUrl)
                .path("greet/Jose")
                .request()
                .get(JsonObject.class);
        Assertions.assertEquals("Hola Jose Carreras!", jsonObject.getString("message"),
                "hola Jose message");

        r = client
                .target(serverUrl)
                .path("metrics")
                .request()
                .get();
        Assertions.assertEquals(200, r.getStatus(), "GET metrics status code");

        r = client
                .target(serverUrl)
                .path("health")
                .request()
                .get();
        Assertions.assertEquals(200, r.getStatus(), "GET health status code");
    }

    @AfterAll
    static void destroyClass() {
        CDI<Object> current = CDI.current();
        ((SeContainer) current).close();
    }
}
