package petclinic;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PetClinicSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    // Сценарий A: чтение данных
    ScenarioBuilder readScenario = scenario("Чтение данных")
        .exec(http("Получить владельцев")
            .get("/api/customer/owners")
            .check(status().is(200)))
        .pause(1)
        .exec(http("Получить ветеринаров")
            .get("/api/vet/vets")
            .check(status().is(200)))
        .pause(1);

    // Сценарий B: создание данных
    ScenarioBuilder writeScenario = scenario("Создание данных")
        .exec(http("Создать владельца")
            .post("/api/customer/owners")
            .body(StringBody(
                "{\"firstName\":\"Нагрузка\",\"lastName\":\"Тест\"," +
                "\"address\":\"ул. Тестовая\",\"city\":\"Казань\"," +
                "\"telephone\":\"89001234567\"}"
            ))
            .check(status().is(201)))
        .pause(1);

    {
        setUp(
            // Сценарий A: плавный рост до 20 пользователей за 30 секунд
            readScenario.injectOpen(
                rampUsers(20).during(30)
            ),
            // Сценарий B: 5 пользователей постоянно в течение 30 секунд
            writeScenario.injectOpen(
                constantUsersPerSec(5).during(30)
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().percentile(95).lt(2000),
            global().failedRequests().percent().lt(5.0)
        );
    }
}
