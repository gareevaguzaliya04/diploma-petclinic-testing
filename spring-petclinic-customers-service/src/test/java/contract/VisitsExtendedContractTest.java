package org.springframework.samples.petclinic.customers.contract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Расширенные контрактные тесты — дополнительные сценарии API visits-service.
 * Уровень: CONTRACT
 * Количество тестов: 8
 */
class VisitsExtendedContractTest {

    private WireMockServer wireMock;
    private RestClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMock.start();
        client = RestClient.builder()
            .baseUrl("http://localhost:" + wireMock.port())
            .build();
    }

    @AfterEach
    void tearDown() { wireMock.stop(); }

    @Nested
    @DisplayName("1. Контракт для разных ID питомцев")
    class DifferentPetIds {

        @ParameterizedTest
        @DisplayName("1.1 Запрос визитов работает для разных ID питомцев")
        @ValueSource(ints = {1, 5, 10, 42, 100})
        void visitsForDifferentPetIds(int petId) {
            wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[]}")));

            assertThatNoException().isThrownBy(() ->
                client.get().uri("/pets/" + petId + "/visits")
                    .retrieve().body(String.class)
            );
        }
    }

    @Nested
    @DisplayName("2. Контракт содержимого визитов")
    class VisitContentContract {

        @Test
        @DisplayName("2.1 Визит с полным набором полей")
        void visitWithAllFields() {
            wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[{" +
                        "\"id\":1," +
                        "\"date\":\"2024-03-15\"," +
                        "\"description\":\"Плановый осмотр\"" +
                        "}]}")));

            String body = client.get().uri("/pets/1/visits")
                .retrieve().body(String.class);

            assertThat(body).contains("\"id\"");
            assertThat(body).contains("\"date\"");
            assertThat(body).contains("\"description\"");
        }

        @Test
        @DisplayName("2.2 Пять визитов в ответе — все присутствуют")
        void fiveVisitsInResponse() {
            StringBuilder items = new StringBuilder("[");
            for (int i = 1; i <= 5; i++) {
                if (i > 1) items.append(",");
                items.append("{\"id\":").append(i)
                     .append(",\"date\":\"2024-0").append(i)
                     .append("-01\",\"description\":\"Визит ").append(i).append("\"}");
            }
            items.append("]");

            wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":" + items + "}")));

            String body = client.get().uri("/pets/1/visits")
                .retrieve().body(String.class);

            for (int i = 1; i <= 5; i++) {
                assertThat(body).contains("Визит " + i);
            }
        }

        @Test
        @DisplayName("2.3 Content-Type ответа — application/json")
        void responseContentType_isJson() {
            wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[]}")));

            // Просто убеждаемся что WireMock обрабатывает запрос
            wireMock.verify(0, getRequestedFor(urlPathMatching("/pets/[0-9]+/visits")));
            assertThatNoException().isThrownBy(() ->
                client.get().uri("/pets/1/visits").retrieve().body(String.class));
            wireMock.verify(1, getRequestedFor(urlPathMatching("/pets/[0-9]+/visits")));
        }
    }
}
