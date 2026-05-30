package org.springframework.samples.petclinic.customers.e2e;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("E2E тестирование — система как чёрный ящик")
@Story("Health checks всех сервисов")
class PetClinicE2ETest {

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String url) throws Exception {
        return http.send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    @Nested
    @DisplayName("1. Health checks")
    class HealthChecks {

        @Test
        @DisplayName("1.1 customers-service здоров")
        @Description("Убеждаемся что customers-service запущен и отдаёт статус UP — базовый smoke-тест, без которого остальные E2E-тесты бессмысленны")
        void customersHealthy() throws Exception {
            var r = get("http://localhost:8081/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }

        @Test
        @DisplayName("1.2 visits-service здоров")
        @Description("Убеждаемся что visits-service запущен — необходим для отображения визитов в профиле питомца, без него customers-service не может показать полные данные")
        void visitsHealthy() throws Exception {
            var r = get("http://localhost:8082/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }

        @Test
        @DisplayName("1.3 vets-service здоров")
        @Description("Убеждаемся что vets-service запущен — необходим для записи питомцев к ветеринару, его падение делает недоступным раздел специалистов")
        void vetsHealthy() throws Exception {
            var r = get("http://localhost:8083/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }

        @Test
        @DisplayName("1.4 api-gateway здоров")
        @Description("Убеждаемся что api-gateway запущен — единая точка входа для всех клиентов, при его падении система полностью недоступна снаружи")
        void gatewayHealthy() throws Exception {
            var r = get("http://localhost:8080/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }
    }

    @Nested
    @DisplayName("2. API Gateway маршрутизация")
    class GatewayRouting {

        @Test
        @DisplayName("2.1 Список владельцев HTTP 200")
        @Description("Gateway должен успешно маршрутизировать запрос /api/customer/owners на customers-service — нарушение маршрута сделает список владельцев недоступным")
        void ownersList_200() throws Exception {
            assertThat(get("http://localhost:8080/api/customer/owners")
                .statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("2.2 Список ветеринаров HTTP 200")
        @Description("Gateway должен успешно маршрутизировать запрос /api/vet/vets на vets-service — нарушение маршрута скроет список специалистов от пользователей")
        void vetsList_200() throws Exception {
            assertThat(get("http://localhost:8080/api/vet/vets")
                .statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("2.3 Ответ содержит JSON")
        @Description("Маршрутизированный ответ должен содержать Content-Type: application/json — иначе клиентское приложение не сможет разобрать список владельцев")
        void response_isJson() throws Exception {
            var r = get("http://localhost:8080/api/customer/owners");
            assertThat(r.headers().firstValue("content-type"))
                .hasValueSatisfying(ct -> assertThat(ct).containsIgnoringCase("json"));
        }

        @Test
        @DisplayName("2.4 Несуществующий маршрут 404")
        @Description("Gateway должен возвращать 404 для несуществующих маршрутов — гарантирует что ошибочные запросы не попадают к внутренним сервисам и не вызывают непредсказуемое поведение")
        void unknownRoute_404() throws Exception {
            assertThat(get("http://localhost:8080/api/nonexistent/xyz")
                .statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("2.5 Тело ответа не пустое")
        @Description("Ответ от customers-service через gateway должен содержать данные — пустое тело означает что маршрутизация работает, но сервис вернул некорректный ответ")
        void responseBody_notEmpty() throws Exception {
            assertThat(get("http://localhost:8080/api/customer/owners")
                .body()).isNotBlank();
        }
    }
}
