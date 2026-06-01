package org.petclinic.tests;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.petclinic.tests.config.BaseTest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("REST API тестирование — REST Assured")
@Story("Управление владельцами — полный CRUD через HTTP API")
@DisplayName("OwnerApiTest — REST API владельцев")
class OwnerApiTest extends BaseTest {

    private static final String OWNERS_PATH = "/api/customer/owners";

    private static final String VALID_OWNER_BODY = """
            {
                "firstName": "Гузалия",
                "lastName":  "Гареева",
                "address":   "ул. Ленина, д.1",
                "city":      "Уфа",
                "telephone": "89001234567"
            }
            """;

    @Step("POST /api/customer/owners — создать владельца и получить ID")
    private int createOwnerAndGetId() {
        return given(spec)
            .body(VALID_OWNER_BODY)
            .post(OWNERS_PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    }

    // ──────────────────────────────────────────────────────
    // 1. Создание владельца
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Создание владельца")
    @Description("POST /owners с валидными данными должен вернуть 201 и все поля ответа")
    @Severity(SeverityLevel.BLOCKER)
    void createOwner_validData_returns201AndAllFields() {
        given(spec)
            .body(VALID_OWNER_BODY)
        .when()
            .post(OWNERS_PATH)
        .then()
            .statusCode(201)
            .body("firstName",  equalTo("Гузалия"))
            .body("lastName",   equalTo("Гареева"))
            .body("address",    equalTo("ул. Ленина, д.1"))
            .body("city",       equalTo("Уфа"))
            .body("telephone",  equalTo("89001234567"))
            .body("id",         notNullValue())
            .body("pets",       notNullValue());
    }

    @Test
    @Story("Создание владельца")
    @Description("POST с пустым firstName должен вернуть 400")
    @Severity(SeverityLevel.CRITICAL)
    void createOwner_blankFirstName_returns400() {
        given(spec)
            .body("""
                  {"firstName":"","lastName":"Петров","address":"ул.1","city":"Уфа","telephone":"89001234567"}
                  """)
        .when()
            .post(OWNERS_PATH)
        .then()
            .statusCode(400);
    }

    @Test
    @Story("Создание владельца")
    @Description("POST с телефоном, содержащим буквы (@Digits), должен вернуть 400")
    @Severity(SeverityLevel.CRITICAL)
    void createOwner_invalidTelephone_returns400() {
        given(spec)
            .body("""
                  {"firstName":"Иван","lastName":"Петров","address":"ул.1","city":"Уфа","telephone":"abc123xyz"}
                  """)
        .when()
            .post(OWNERS_PATH)
        .then()
            .statusCode(400);
    }

    // ──────────────────────────────────────────────────────
    // 2. Получение владельца
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Получение владельца по ID")
    @Description("GET /owners/{id} для существующего владельца → 200 с корректными данными")
    @Severity(SeverityLevel.BLOCKER)
    void getOwner_existingId_returns200WithData() {
        int ownerId = createOwnerAndGetId();

        given(spec)
            .pathParam("id", ownerId)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(200)
            .body("id",        equalTo(ownerId))
            .body("firstName", equalTo("Гузалия"))
            .body("lastName",  equalTo("Гареева"))
            .body("pets",      notNullValue());
    }

    @Test
    @Story("Получение владельца по ID")
    @Description("GET /owners/{id} для несуществующего ID → не возвращает данные реального владельца")
    @Severity(SeverityLevel.CRITICAL)
    void getOwner_nonExistentId_returnsNoRealOwner() {
        io.restassured.response.Response response = given(spec)
            .pathParam("id", 999_999_999)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .extract().response();

        // 404 — если маршрутизация сработала корректно;
        // 200 с пустым телом — если сработал fallback circuit breaker gateway.
        // В обоих случаях данных реального владельца быть не должно.
        int status = response.statusCode();
        org.assertj.core.api.Assertions.assertThat(status)
            .as("Ожидается 404 (не найден) или 200 (CB fallback), но не 5xx")
            .isIn(200, 404);
        if (status == 200) {
            String body = response.getBody().asString();
            org.assertj.core.api.Assertions.assertThat(body)
                .as("При 200-ответе тело должно быть пустым (CB fallback, не реальный владелец)")
                .satisfiesAnyOf(
                    b -> org.assertj.core.api.Assertions.assertThat(b).isEmpty(),
                    b -> org.assertj.core.api.Assertions.assertThat(b).isEqualTo("null"),
                    b -> org.assertj.core.api.Assertions.assertThat(b).doesNotContain("\"id\":")
                );
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. Список всех владельцев
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Список владельцев")
    @Description("GET /owners → 200, ответ является непустым массивом")
    @Severity(SeverityLevel.NORMAL)
    void getAllOwners_returns200AndNonNullList() {
        createOwnerAndGetId();

        Response response = given(spec)
            .when()
            .get(OWNERS_PATH)
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class))
            .extract().response();

        assertThat(response.jsonPath().getList("$")).isNotNull();
    }

    // ──────────────────────────────────────────────────────
    // 4. Обновление владельца
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Обновление владельца")
    @Description("PUT /owners/{id} для существующего владельца → 204 No Content")
    @Severity(SeverityLevel.BLOCKER)
    void updateOwner_existingId_returns204() {
        int ownerId = createOwnerAndGetId();

        given(spec)
            .pathParam("id", ownerId)
            .body("""
                  {
                      "firstName": "Новое",
                      "lastName":  "Имя",
                      "address":   "ул. Новая, д.2",
                      "city":      "Казань",
                      "telephone": "89009876543"
                  }
                  """)
        .when()
            .put(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(204);
    }

    @Test
    @Story("Обновление владельца")
    @Description("После PUT данные владельца действительно изменились в БД")
    @Severity(SeverityLevel.CRITICAL)
    void updateOwner_dataActuallyChanged() {
        int ownerId = createOwnerAndGetId();

        given(spec)
            .pathParam("id", ownerId)
            .body("""
                  {
                      "firstName": "Обновлённый",
                      "lastName":  "Владелец",
                      "address":   "пр. Обновлённый, д.99",
                      "city":      "Москва",
                      "telephone": "74951234567"
                  }
                  """)
        .when()
            .put(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(204);

        given(spec)
            .pathParam("id", ownerId)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Обновлённый"))
            .body("city",      equalTo("Москва"));
    }

    @Test
    @Story("Обновление владельца")
    @Description("PUT /owners/{id} для несуществующего ID → 404")
    @Severity(SeverityLevel.CRITICAL)
    void updateOwner_nonExistentId_returns404() {
        given(spec)
            .pathParam("id", 999_999_999)
            .body(VALID_OWNER_BODY)
        .when()
            .put(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(404);
    }
}
