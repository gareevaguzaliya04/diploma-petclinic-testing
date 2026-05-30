package org.petclinic.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.petclinic.tests.config.BaseTest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Полный сценарий жизненного цикла: создание владельца → питомец → обновление → проверка.
 * Один тест-метод + @Step методы = полноценный Allure-отчёт с деревом шагов.
 */
@Epic("Стратегия тестирования микросервисов")
@Feature("REST API тестирование — REST Assured")
@DisplayName("FullScenarioTest — жизненный цикл")
class FullScenarioTest extends BaseTest {

    private static final String OWNERS_PATH = "/api/customer/owners";

    // ── Хранимое состояние сценария ────────────────────────
    private int ownerId;
    private int petId;

    // ── Точка входа в сценарий ────────────────────────────

    @Test
    @Story("Сквозной сценарий: владелец → питомец → визит → обновление")
    @Description("""
            Шаг 1: Создать владельца
            Шаг 2: Проверить что владелец создан (GET by ID)
            Шаг 3: Добавить питомца владельцу
            Шаг 4: Получить владельца и убедиться, что питомец присутствует
            Шаг 5: Обновить данные владельца (PUT)
            Шаг 6: Проверить что данные обновились
            Шаг 7: Запросить несуществующего владельца → 404
            """)
    @Severity(SeverityLevel.BLOCKER)
    void fullOwnerPetLifecycle() {
        step1_createOwner();
        step2_verifyOwnerCreated();
        step3_addPetToOwner();
        step4_verifyPetBelongsToOwner();
        step5_updateOwnerData();
        step6_verifyOwnerDataUpdated();
        step7_nonExistentOwnerReturns404();
    }

    // ── Шаги сценария ─────────────────────────────────────

    @Step("Шаг 1: Создать владельца через POST /api/customer/owners")
    private void step1_createOwner() {
        String body = """
                {
                    "firstName": "Сценарный",
                    "lastName":  "Владелец",
                    "address":   "ул. Тестовая, д.1",
                    "city":      "Уфа",
                    "telephone": "89001112233"
                }
                """;

        ownerId = given(spec)
            .body(body)
        .when()
            .post(OWNERS_PATH)
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .path("id");

        assertThat(ownerId).isPositive();
        Allure.addAttachment("Создан владелец с ID", String.valueOf(ownerId));
    }

    @Step("Шаг 2: Проверить, что владелец существует GET /api/customer/owners/{id}")
    private void step2_verifyOwnerCreated() {
        given(spec)
            .pathParam("id", ownerId)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(200)
            .body("id",        equalTo(ownerId))
            .body("firstName", equalTo("Сценарный"))
            .body("lastName",  equalTo("Владелец"))
            .body("pets",      notNullValue());
    }

    @Step("Шаг 3: Добавить питомца владельцу POST /api/customer/owners/{id}/pets")
    private void step3_addPetToOwner() {
        String petBody = """
                {
                    "id":        0,
                    "name":      "Пушок",
                    "birthDate": "2021-03-20",
                    "typeId":    1
                }
                """;

        petId = given(spec)
            .body(petBody)
        .when()
            .post(OWNERS_PATH + "/{ownerId}/pets", ownerId)
        .then()
            .statusCode(201)
            .body("id",   notNullValue())
            .body("name", equalTo("Пушок"))
            .extract()
            .path("id");

        assertThat(petId).isPositive();
        Allure.addAttachment("Создан питомец с ID", String.valueOf(petId));
    }

    @Step("Шаг 4: Получить владельца и убедиться, что питомец 'Пушок' присутствует")
    private void step4_verifyPetBelongsToOwner() {
        var petsNames = given(spec)
            .pathParam("id", ownerId)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("pets.name", String.class);

        assertThat(petsNames)
            .as("Питомец 'Пушок' должен быть в списке питомцев владельца")
            .contains("Пушок");
    }

    @Step("Шаг 5: Обновить данные владельца PUT /api/customer/owners/{id}")
    private void step5_updateOwnerData() {
        String updateBody = """
                {
                    "firstName": "Обновлённый",
                    "lastName":  "Владелец",
                    "address":   "ул. Новая, д.99",
                    "city":      "Казань",
                    "telephone": "89009998877"
                }
                """;

        given(spec)
            .pathParam("id", ownerId)
            .body(updateBody)
        .when()
            .put(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(204);
    }

    @Step("Шаг 6: Проверить, что данные владельца обновились")
    private void step6_verifyOwnerDataUpdated() {
        given(spec)
            .pathParam("id", ownerId)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Обновлённый"))
            .body("city",      equalTo("Казань"))
            .body("telephone", equalTo("89009998877"));
    }

    @Step("Шаг 7: Запрос несуществующего владельца → 404 Not Found")
    private void step7_nonExistentOwnerReturns404() {
        given(spec)
            .pathParam("id", 999_999_999)
        .when()
            .get(OWNERS_PATH + "/{id}")
        .then()
            .statusCode(404);
    }
}
