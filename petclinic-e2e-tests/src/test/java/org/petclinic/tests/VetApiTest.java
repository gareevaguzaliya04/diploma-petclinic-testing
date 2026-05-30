package org.petclinic.tests;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.petclinic.tests.config.BaseTest;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("REST API тестирование — REST Assured")
@Story("Управление ветеринарами — просмотр справочника специалистов")
@DisplayName("VetApiTest — REST API ветеринаров")
class VetApiTest extends BaseTest {

    private static final String VETS_PATH = "/api/vet/vets";

    @Step("GET /api/vet/vets — получить список ветеринаров")
    private Response fetchVets() {
        return given(spec)
            .when()
            .get(VETS_PATH)
            .then()
            .statusCode(200)
            .extract().response();
    }

    @Test
    @Story("Список ветеринаров")
    @Description("GET /vets → 200, ответ является массивом JSON")
    @Severity(SeverityLevel.BLOCKER)
    void getVets_returns200AndJsonArray() {
        given(spec)
        .when()
            .get(VETS_PATH)
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("$", instanceOf(List.class));
    }

    @Test
    @Story("Список ветеринаров")
    @Description("Список ветеринаров не пуст — данные загружены из data.sql")
    @Severity(SeverityLevel.CRITICAL)
    void getVets_listIsNotEmpty() {
        Response response = fetchVets();

        List<?> vets = response.jsonPath().getList("$");
        assertThat(vets).isNotEmpty();
    }

    @Test
    @Story("Структура данных ветеринара")
    @Description("Каждый ветеринар содержит поля: firstName, lastName, specialties, nrOfSpecialties")
    @Severity(SeverityLevel.CRITICAL)
    void getVets_eachVetHasRequiredFields() {
        Response response = fetchVets();

        List<Map<String, Object>> vets = response.jsonPath().getList("$");
        assertThat(vets).isNotEmpty();

        vets.forEach(vet -> {
            assertThat(vet).containsKey("firstName");
            assertThat(vet).containsKey("lastName");
            assertThat(vet).containsKey("specialties");
            assertThat(vet).containsKey("nrOfSpecialties");
            assertThat(vet.get("firstName")).isNotNull();
            assertThat(vet.get("lastName")).isNotNull();
            assertThat(vet.get("specialties")).isInstanceOf(List.class);
        });
    }
}
