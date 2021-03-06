package io.quarkuscoffeeshop.web.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkuscoffeeshop.domain.Item;
import io.quarkuscoffeeshop.domain.OrderStatus;
import io.quarkuscoffeeshop.web.domain.DashboardUpdate;
import io.quarkuscoffeeshop.web.testing.KafkaTestResource;
import io.smallrye.reactive.messaging.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.connectors.InMemorySource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@TestHTTPEndpoint(DashboardResource.class)
public class DashboardResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardResourceTest.class);

    private static final String expectedPayload = "data: {\"orderId\":\"82124c69-a108-4ccc-9ac4-64566e389178\",\"itemId\":\"f84cb5e2-a3fd-43af-8df8-b5d74b133115\",\"name\":\"Scotty\",\"item\":\"COFFEE_WITH_ROOM\",\"status\":\"IN_QUEUE\",\"madeBy\":\"\"}";

    @Inject
    @Any
    InMemoryConnector connector;

    @Test
    public void testStreaming() {

        try {
            DashboardUpdate dashboardUpdate = new DashboardUpdate(
                    "82124c69-a108-4ccc-9ac4-64566e389178",
                    "f84cb5e2-a3fd-43af-8df8-b5d74b133115",
                    "Scotty",
                    Item.COFFEE_WITH_ROOM,
                    OrderStatus.IN_QUEUE,
                    Optional.empty()
            );
            InMemorySource<String> ordersIn = connector.source("web-updates");
            ordersIn.send(JsonUtil.toJson(dashboardUpdate));
            await().atLeast(2, TimeUnit.SECONDS);

            URI uri = new URI("http://localhost:8080/dashboard/stream");
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder(uri).GET().build();
            Stream<String> lines = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines()).body();

            Optional<String> payload = lines.filter(l -> l.length() >= 1).findFirst();
            assertTrue(payload.isPresent());
            assertEquals(expectedPayload, payload.get());
        } catch (URISyntaxException e) {
            assertNull(e, "Exception thrown");
        } catch (IOException e) {
            assertNull(e, "Exception thrown");
        } catch (InterruptedException e) {
            assertNull(e, "Exception thrown");
        }

    }

}
