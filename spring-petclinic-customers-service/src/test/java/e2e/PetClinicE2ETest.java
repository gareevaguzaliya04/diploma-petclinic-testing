package org.springframework.samples.petclinic.customers.e2e;

import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;

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
        void customersHealthy() throws Exception {
            var r = get("http://localhost:8081/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }

        @Test
        @DisplayName("1.2 visits-service здоров")
        void visitsHealthy() throws Exception {
            var r = get("http://localhost:8082/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }

        @Test
        @DisplayName("1.3 vets-service здоров")
        void vetsHealthy() throws Exception {
            var r = get("http://localhost:8083/actuator/health");
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.body()).contains("UP");
        }

        @Test
        @DisplayName("1.4 api-gateway здоров")
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
        void ownersList_200() throws Exception {
            assertThat(get("http://localhost:8080/api/customer/owners")
                .statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("2.2 Список ветеринаров HTTP 200")
        void vetsList_200() throws Exception {
            assertThat(get("http://localhost:8080/api/vet/vets")
                .statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("2.3 Ответ содержит JSON")
        void response_isJson() throws Exception {
            var r = get("http://localhost:8080/api/customer/owners");
            assertThat(r.headers().firstValue("content-type"))
                .hasValueSatisfying(ct -> assertThat(ct).containsIgnoringCase("json"));
        }

        @Test
        @DisplayName("2.4 Несуществующий маршрут 404")
        void unknownRoute_404() throws Exception {
            assertThat(get("http://localhost:8080/api/nonexistent/xyz")
                .statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("2.5 Тело ответа не пустое")
        void responseBody_notEmpty() throws Exception {
            assertThat(get("http://localhost:8080/api/customer/owners")
                .body()).isNotBlank();
        }
    }
}