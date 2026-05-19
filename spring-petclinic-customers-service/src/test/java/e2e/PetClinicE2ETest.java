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
        return client.send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    @Test
    @DisplayName("customers-service health check")
    void shouldCustomersServiceBeHealthy() throws Exception {
        var response = get("http://localhost:8081/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("visits-service health check")
    void shouldVisitsServiceBeHealthy() throws Exception {
        var response = get("http://localhost:8082/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("vets-service health check")
    void shouldVetsServiceBeHealthy() throws Exception {
        var response = get("http://localhost:8083/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("API Gateway returns owners list")
    void shouldReturnOwnersList() throws Exception {
        var response = get("http://localhost:8080/api/customer/owners");
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
