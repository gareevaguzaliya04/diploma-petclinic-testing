package org.springframework.samples.petclinic.customers.contract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Контрактное тестирование — WireMock")
@Story("Критический контракт API visits-service")
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
        @Description("visits-service обязан возвращать обёртку с полем 'items' — если поле будет переименовано, customers-service получит NPE при десериализации и перестанет отображать визиты в профиле питомца")
        void responseContainsItemsField() {
            stubVisits("[{\"id\":1,\"date\":\"2024-01-01\",\"description\":\"osmotr\"}]");
            assertThat(getVisits(1)).contains("\"items\"");
        }

        @Test
        @DisplayName("1.2 Поле date присутствует")
        @Description("Поле 'date' — критичный контракт: если visits-service изменит название поля (например на 'visitDate'), UI клиники перестанет отображать дату визита, что нарушит сценарий просмотра истории болезней питомца")
        void visitHasDateField() {
            stubVisits("[{\"id\":1,\"date\":\"2024-06-15\",\"description\":\"vakcinaciya\"}]");
            assertThat(getVisits(1)).contains("\"date\"");
        }

        @Test
        @DisplayName("1.3 Поле description присутствует")
        @Description("Поле 'description' должно присутствовать — его отсутствие или переименование в visits-service лишит врачей возможности видеть причину визита в интерфейсе клиники")
        void visitHasDescriptionField() {
            stubVisits("[{\"id\":1,\"date\":\"2024-06-15\",\"description\":\"osmotr\"}]");
            assertThat(getVisits(1)).contains("\"description\"");
        }

        @Test
        @DisplayName("1.4 Поле id присутствует")
        @Description("Поле 'id' визита необходимо для ссылок и навигации — его отсутствие сломает переходы к конкретному визиту из профиля питомца")
        void visitHasIdField() {
            stubVisits("[{\"id\":42,\"date\":\"2024-06-15\",\"description\":\"osmotr\"}]");
            assertThat(getVisits(1)).contains("\"id\"");
        }

        @Test
        @DisplayName("1.5 Пустой список валидный ответ")
        @Description("visits-service должен возвращать пустой массив items, а не null — иначе customers-service получит NullPointerException при попытке итерации по визитам питомца")
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
        @Description("При наличии нескольких визитов все они должны присутствовать в ответе — потеря данных недопустима для медицинской истории питомца")
        void twoVisits_bothPresent() {
            stubVisits("[{\"id\":1,\"date\":\"2024-01-15\",\"description\":\"osmotr\"},"
                + "{\"id\":2,\"date\":\"2024-06-10\",\"description\":\"vaccine\"}]");
            String body = getVisits(1);
            assertThat(body).contains("osmotr").contains("vaccine");
        }

        @Test
        @DisplayName("2.2 Описание передается без искажений")
        @Description("Текст описания визита должен передаваться точно — искажение медицинских записей недопустимо и нарушает целостность данных между сервисами")
        void visitDescription_correct() {
            stubVisits("[{\"id\":1,\"date\":\"2024-01-01\",\"description\":\"annual checkup\"}]");
            assertThat(getVisits(1)).contains("annual checkup");
        }

        @Test
        @DisplayName("2.3 Запрос к WireMock выполнен")
        @Description("customers-service должен выполнять HTTP-запрос к visits-service — проверяем что интеграционный вызов действительно происходит, а не подменяется локальным кэшем")
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
        @Description("При недоступности visits-service (503) клиент должен получить исключение — это позволяет customers-service применить стратегию fallback и не показывать пустой раздел визитов")
        void serviceUnavailable_throws() {
            wireMock.stubFor(get(urlPathMatching("/pets/[0-9]+/visits"))
                .willReturn(aResponse().withStatus(503)));
            assertThatThrownBy(() -> getVisits(1)).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("3.2 Успешный запрос без исключений")
        @Description("Нормальный ответ от visits-service не должен вызывать исключений — гарантируем что контракт работает без сбоев в штатном режиме")
        void successfulRequest_noException() {
            stubVisits("[]");
            assertThatNoException().isThrownBy(() -> getVisits(1));
        }
    }
}
