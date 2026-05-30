package org.petclinic.tests.config;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;

/**
 * Базовый класс для всех E2E тестов.
 * Настраивает REST Assured, Allure-фильтр и ожидает готовности сервисов.
 */
public abstract class BaseTest {

    /** Базовый URL API Gateway. Переопределяется через -Dbase.url=... */
    protected static final String BASE_URL =
        System.getProperty("base.url", "http://localhost:8080");

    protected static RequestSpecification spec;

    @BeforeAll
    static void globalSetup() {
        waitForGateway();
        spec = new RequestSpecBuilder()
            .setBaseUri(BASE_URL)
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())    // прикрепляет запрос/ответ к Allure
            .addFilter(new RequestLoggingFilter()) // stdout-лог для отладки
            .addFilter(new ResponseLoggingFilter())
            .build();
    }

    /**
     * Ждёт, пока API Gateway ответит 200 на /actuator/health.
     * Полезно при cold-start контейнеров в CI.
     */
    private static void waitForGateway() {
        Awaitility.await()
            .alias("API Gateway health check")
            .atMost(Duration.ofSeconds(180))
            .pollInterval(Duration.ofSeconds(5))
            .ignoreExceptions()
            .until(() -> {
                int status = RestAssured.given()
                    .get(BASE_URL + "/actuator/health")
                    .getStatusCode();
                return status == 200;
            });
    }
}
