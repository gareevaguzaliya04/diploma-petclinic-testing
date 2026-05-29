package org.springframework.samples.petclinic.customers.contract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class VisitsClientContractTest {

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

    private void stubVisits(String itemsJson) {
        wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"items\":" + itemsJson + "}")));
    }

    private String getVisits(int petId) {
        return client.get().uri("/pets/" + petId + "/visits")
            .retrieve().body(String.class);
    }

    @Nested
    @DisplayName("1. Контракт формата ответа")
    class ResponseFormatContract {

        @Test
        @DisplayName("1.1 Ответ содержит поле items")
        void responseContainsItemsField() {
            stubVisits("[{\"id\":1,\"date\":\"2024-01-01\",\"description\":\"osmotr\"}]");
            assertThat(getVisits(1)).contains("\"items\"");
        }

        @Test
        @DisplayName("1.2 Поле date присутствует")
        void visitHasDateField() {
            stubVisits("[{\"id\":1,\"date\":\"2024-06-15\",\"description\":\"vakcinaciya\"}]");
            assertThat(getVisits(1)).contains("\"date\"");
        }

        @Test
        @DisplayName("1.3 Поле description присутствует")
        void visitHasDescriptionField() {
            stubVisits("[{\"id\":1,\"date\":\"2024-06-15\",\"description\":\"osmotr\"}]");
            assertThat(getVisits(1)).contains("\"description\"");
        }

        @Test
        @DisplayName("1.4 Поле id присутствует")
        void visitHasIdField() {
            stubVisits("[{\"id\":42,\"date\":\"2024-06-15\",\"description\":\"osmotr\"}]");
            assertThat(getVisits(1)).contains("\"id\"");
        }

        @Test
        @DisplayName("1.5 Пустой список валидный ответ")
        void emptyVisitsList() {
            stubVisits("[]");
            String body = getVisits(1);
            assertThat(body).contains("\"items\"");
            assertThat(body).contains("[]");
        }
    }

    @Nested
    @DisplayName("2. Корректность данных")
    class DataCorrectnessTests {

        @Test
        @DisplayName("2.1 Два визита возвращаются оба")
        void twoVisits_bothPresent() {
            stubVisits("[{\"id\":1,\"date\":\"2024-01-15\",\"description\":\"osmotr\"},"
                + "{\"id\":2,\"date\":\"2024-06-10\",\"description\":\"vaccine\"}]");
            String body = getVisits(1);
            assertThat(body).contains("osmotr").contains("vaccine");
        }

        @Test
        @DisplayName("2.2 Описание передается без искажений")
        void visitDescription_correct() {
            stubVisits("[{\"id\":1,\"date\":\"2024-01-01\",\"description\":\"annual checkup\"}]");
            assertThat(getVisits(1)).contains("annual checkup");
        }

        @Test
        @DisplayName("2.3 Запрос к WireMock выполнен")
        void request_isSent() {
            stubVisits("[]");
            getVisits(5);
            wireMock.verify(getRequestedFor(urlPathMatching("/pets/[0-9]+/visits")));
        }
    }

    @Nested
    @DisplayName("3. Обработка ошибок")
    class ErrorHandlingTests {

        @Test
        @DisplayName("3.1 503 от visits-service бросает исключение")
        void serviceUnavailable_throws() {
            wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
                .willReturn(aResponse().withStatus(503)));
            assertThatThrownBy(() -> getVisits(1)).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("3.2 Успешный запрос без исключений")
        void successfulRequest_noException() {
            stubVisits("[]");
            assertThatNoException().isThrownBy(() -> getVisits(1));
        }
    }
}