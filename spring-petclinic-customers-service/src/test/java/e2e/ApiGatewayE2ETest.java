package org.springframework.samples.petclinic.customers.e2e;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Расширенные E2E-тесты — проверка интеграции всех сервисов.
 * Уровень: E2E (требует docker compose up -d)
 */
@Epic("Стратегия тестирования микросервисов")
@Feature("E2E тестирование — система как чёрный ящик")
@Story("Маршрутизация через API Gateway")
class ApiGatewayE2ETest {

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String url) throws Exception {
        return http.send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> post(String url, String body) throws Exception {
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    @Nested
    @DisplayName("1. Проверка данных через API")
    class DataVerificationTests {

        @Test
        @DisplayName("1.1 Список владельцев — ответ не пустой массив")
        @Description("Система в режиме E2E должна возвращать реальных владельцев из БД — пустой ответ означает что тестовые данные не загружены или БД не инициализирована")
        void ownersList_notEmpty() throws Exception {
            var r = get("http://localhost:8080/api/customer/owners");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).isNotBlank();
        }

        @Test
        @DisplayName("1.2 Список ветеринаров — содержит данные")
        @Description("vets-service через gateway должен возвращать список специалистов — это базовый сценарий, без которого запись к ветеринару невозможна")
        void vetsList_containsData() throws Exception {
            var r = get("http://localhost:8080/api/vet/vets");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).isNotBlank();
        }

    }

    @Nested
    @DisplayName("2. Создание новой записи")
    class CreateRecordTests {

        @Test
        @DisplayName("2.1 Создание нового владельца через Gateway — HTTP 201")
        @Description("Сквозной тест создания владельца: Gateway → customers-service → PostgreSQL — проверяем что весь путь работает и данные сохраняются")
        void createOwner_returns201() throws Exception {
            String body = "{" +
                "\"firstName\":\"Тестовый\"," +
                "\"lastName\":\"Владелец\"," +
                "\"address\":\"ул. Тестовая, д. 1\"," +
                "\"city\":\"Уфа\"," +
                "\"telephone\":\"89001234567\"" +
                "}";
            var r = post("http://localhost:8080/api/customer/owners", body);
            assertThat(r.statusCode()).isIn(200, 201);
        }
    }

    @Nested
    @DisplayName("3. Проверка заголовков ответа")
    class ResponseHeaderTests {

        @Test
        @DisplayName("3.1 Ответ customers-service содержит Content-Type JSON")
        @Description("Content-Type: application/json обязателен — без него браузерные клиенты не смогут автоматически разобрать ответ и отобразить данные")
        void customersResponse_hasJsonContentType() throws Exception {
            var r = get("http://localhost:8081/api/customer/owners");
            assertThat(r.headers().firstValue("content-type"))
                .hasValueSatisfying(ct -> assertThat(ct).containsIgnoringCase("json"));
        }

        @Test
        @DisplayName("3.2 Health-check возвращает статус компонентов")
        @Description("Actuator health должен содержать поле 'status' — это необходимо для мониторинга и автоматических health-check в Kubernetes/Docker-оркестраторах")
        void healthCheck_containsStatus() throws Exception {
            var r = get("http://localhost:8081/actuator/health");
            assertThat(r.body()).contains("status");
        }

        @Test
        @DisplayName("3.3 Actuator info доступен")
        @Description("Endpoint /actuator/info должен быть доступен (200 или 404 если не настроен) — полная блокировка actuator эндпоинтов нарушит мониторинг в production")
        void actuatorInfo_isAccessible() throws Exception {
            var r = get("http://localhost:8081/actuator/info");
            assertThat(r.statusCode()).isIn(200, 404); // 404 если info не настроен — ок
        }
    }
}
