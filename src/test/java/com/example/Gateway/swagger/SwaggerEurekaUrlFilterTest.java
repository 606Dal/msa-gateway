package com.example.Gateway.swagger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",
                "spring.cloud.discovery.client.simple.instances.member-service[0].uri=http://127.0.0.1:9001",
                "spring.cloud.discovery.client.simple.instances.order-service[0].uri=http://127.0.0.1:9002",
                "spring.cloud.discovery.client.simple.instances.gateway[0].uri=http://127.0.0.1:8000",
                "spring.cloud.discovery.client.simple.instances.eureka-server[0].uri=http://127.0.0.1:8761"
        }
)
class SwaggerEurekaUrlFilterTest {

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void swaggerConfigListsRegisteredServices() {
        webTestClient.get()
                .uri("/index.html")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", "/swagger-ui/index.html");

        webTestClient.get()
                .uri("/v3/api-docs/swagger-config")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.url").doesNotExist()
                .jsonPath("$.urls.length()").isEqualTo(2)
                .jsonPath("$.urls[0].name").isEqualTo("member-service")
                .jsonPath("$.urls[0].url").isEqualTo("/member-service/v3/api-docs")
                .jsonPath("$.urls[1].name").isEqualTo("order-service")
                .jsonPath("$.urls[1].url").isEqualTo("/order-service/v3/api-docs")
                .jsonPath("$['urls.primaryName']").isEqualTo("member-service");

        webTestClient.get()
                .uri("/member-service/v3/api-docs")
                .exchange()
                .expectStatus().value(status -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        404,
                        status,
                        "discovery route for the service OpenAPI document was not registered"
                ));
    }
}
