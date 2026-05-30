package org.petclinic.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.petclinic.tests.config.BaseTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Customers Service")
@Feature("Управление питомцами (Pets API)")
@DisplayName("PetApiTest — REST API питомцев")
class PetApiTest extends BaseTest {

    private static final String OWNERS_PATH   = "/api/customer/owners";
    private static final String PET_TYPES_PATH = "/api/customer/petTypes";

    private static final String OWNER_BODY = """
            {
                "firstName": "Владелец",
                "lastName":  "Питомца",
                "address":   "ул. Зоологическая, д.1",
                "city":      "Уфа",
                "telephone": "89007654321"
            }
            """;

    /** birthDate в формате yyyy-MM-dd (Jackson @JsonFormat в PetRequest) */
    private static final String PET_BODY = """
            {
                "id":        0,
                "name":      "Барсик",
                "birthDate": "2020-06-15",
                "typeId":    1
            }
            """;

    private int createOwner() {
        return given(spec)
            .body(OWNER_BODY)
            .post(OWNERS_PATH)
            .then().statusCode(201)
            .extract().path("id");
    }

    private int createPet(int ownerId) {
        return given(spec)
            .body(PET_BODY)
            .post(OWNERS_PATH + "/{ownerId}/pets", ownerId)
            .then().statusCode(201)
            .extract().path("id");
    }

    // ──────────────────────────────────────────────────────
    // 1. Типы питомцев
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Типы питомцев")
    @Description("GET /petTypes → 200, список не пустой, у каждого типа есть id и name")
    @Severity(SeverityLevel.CRITICAL)
    void getPetTypes_returns200WithNonEmptyList() {
        given(spec)
        .when()
            .get(PET_TYPES_PATH)
        .then()
            .statusCode(200)
            .body("$",         not(empty()))
            .body("[0].id",    notNullValue())
            .body("[0].name",  notNullValue());
    }

    // ──────────────────────────────────────────────────────
    // 2. Создание питомца
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Создание питомца")
    @Description("POST /owners/{id}/pets → 201 с данными питомца (name, type, birthDate)")
    @Severity(SeverityLevel.BLOCKER)
    void createPet_validData_returns201() {
        int ownerId = createOwner();

        given(spec)
            .body(PET_BODY)
        .when()
            .post(OWNERS_PATH + "/{ownerId}/pets", ownerId)
        .then()
            .statusCode(201)
            .body("id",        notNullValue())
            .body("name",      equalTo("Барсик"))
            .body("birthDate", notNullValue());
    }

    @Test
    @Story("Создание питомца")
    @Description("POST /owners/{id}/pets для несуществующего владельца → 404")
    @Severity(SeverityLevel.CRITICAL)
    void createPet_nonExistentOwner_returns404() {
        given(spec)
            .body(PET_BODY)
        .when()
            .post(OWNERS_PATH + "/{ownerId}/pets", 999_999_999)
        .then()
            .statusCode(404);
    }

    // ──────────────────────────────────────────────────────
    // 3. Обновление питомца
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Обновление питомца")
    @Description("PUT /owners/*/pets/{id} → 204 No Content")
    @Severity(SeverityLevel.BLOCKER)
    void updatePet_existingPet_returns204() {
        int ownerId = createOwner();
        int petId   = createPet(ownerId);

        String updateBody = String.format("""
                {
                    "id":        %d,
                    "name":      "НовоеИмяПитомца",
                    "birthDate": "2019-03-10",
                    "typeId":    2
                }
                """, petId);

        given(spec)
            .body(updateBody)
        .when()
            .put(OWNERS_PATH + "/1/pets/{petId}", petId)
        .then()
            .statusCode(204);
    }

    // ──────────────────────────────────────────────────────
    // 4. Получение питомца
    // ──────────────────────────────────────────────────────

    @Test
    @Story("Получение питомца по ID")
    @Description("GET /owners/*/pets/{id} → 200 с полями id, name, birthDate, type")
    @Severity(SeverityLevel.BLOCKER)
    void getPet_existingPet_returns200WithFields() {
        int ownerId = createOwner();
        int petId   = createPet(ownerId);

        given(spec)
        .when()
            .get(OWNERS_PATH + "/1/pets/{petId}", petId)
        .then()
            .statusCode(200)
            .body("id",        equalTo(petId))
            .body("name",      equalTo("Барсик"))
            .body("birthDate", notNullValue());
    }
}
