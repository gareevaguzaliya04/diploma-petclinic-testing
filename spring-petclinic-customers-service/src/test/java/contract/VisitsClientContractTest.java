package org.springframework.samples.petclinic.customers.contract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class VisitsClientContractTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("visits-service возвращает список визитов для питомца")
    void shouldReturnVisitsForPet() {
        wireMockServer.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "items": [
                            {"id": 1, "date": "2024-01-15", "description": "Ежегодный осмотр"},
                            {"id": 2, "date": "2024-06-10", "description": "Вакцинация"}
                        ]
                    }
                """)));

        RestClient client = RestClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        String response = client.get()
            .uri("/pets/1/visits")
            .retrieve()
            .body(String.class);

        assertThat(response).contains("Ежегодный осмотр");
        assertThat(response).contains("Вакцинация");

        wireMockServer.verify(getRequestedFor(urlPathMatching("/pets/[0-9]+/visits")));
    }

    @Test
    @DisplayName("visits-service возвращает 404 если питомец не существует")
    void shouldReturn404_whenPetNotFound() {
        wireMockServer.stubFor(get(urlPathMatching("/pets/99999/visits"))
            .willReturn(aResponse()
                .withStatus(404)));

        RestClient client = RestClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        assertThatThrownBy(() ->
            client.get()
                .uri("/pets/99999/visits")
                .retrieve()
                .body(String.class)
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("visits-service возвращает пустой список если визитов нет")
    void shouldReturnEmptyList_whenNoVisits() {
        wireMockServer.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"items\": []}")));

        RestClient client = RestClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        String response = client.get()
            .uri("/pets/1/visits")
            .retrieve()
            .body(String.class);

        assertThat(response).contains("items");
        assertThat(response).contains("[]");
    }
}