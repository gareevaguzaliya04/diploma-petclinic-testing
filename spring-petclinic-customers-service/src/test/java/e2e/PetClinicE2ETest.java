package org.springframework.samples.petclinic.customers.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;

class PetClinicE2ETest {

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("customers-service health-check работает")
    void shouldCustomersServiceBeHealthy() throws Exception {
        var response = get("http://localhost:8081/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("visits-service health-check работает")
    void shouldVisitsServiceBeHealthy() throws Exception {
        var response = get("http://localhost:8082/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("vets-service health-check работает")
    void shouldVetsServiceBeHealthy() throws Exception {
        var response = get("http://localhost:8083/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("API Gateway возвращает список владельцев")
    void shouldReturnOwnersList() throws Exception {
        var response = get("http://localhost:8080/api/customer/owners");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotEmpty();
    }

    @Test
    @DisplayName("Создание нового владельца через API")
    void shouldCreateNewOwner() throws Exception {
        String body = "{\"firstName\":\"Тест\",\"lastName\":\"Тестов\"," +
            "\"address\":\"ул. Тестовая 1\",\"city\":\"Казань\"," +
            "\"telephone\":\"89001112233\"}";
        var response = post("http://localhost:8080/api/customer/owners", body);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("Тест");
    }
}
