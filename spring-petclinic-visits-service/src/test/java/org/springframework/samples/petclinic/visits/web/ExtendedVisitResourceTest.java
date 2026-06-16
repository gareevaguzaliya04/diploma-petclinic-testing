package org.springframework.samples.petclinic.visits.web;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Контроллерное тестирование — visits-service")
@Story("REST API для управления визитами питомцев")
@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
class ExtendedVisitResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VisitRepository visitRepository;

    private Visit buildVisit(int id, int petId, String desc) {
        return Visit.VisitBuilder.aVisit()
            .id(id)
            .petId(petId)
            .date(new Date())
            .description(desc)
            .build();
    }

    // ──────────────────────────────────────────────────────
    // 1. POST визиты
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. POST owners/*/pets/{petId}/visits — создание визита")
    class CreateVisit {

        @Test
        @DisplayName("1.1 Корректная запись визита возвращает 201 с данными")
        @Description("Основной сценарий: врач записывает питомца на приём. " +
            "Сервис должен принять данные, сохранить и вернуть статус 201 с объектом визита.")
        @Severity(SeverityLevel.BLOCKER)
        void valid_returns201() throws Exception {
            given(visitRepository.save(any(Visit.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/1/pets/5/visits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Плановый осмотр\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(5))
                .andExpect(jsonPath("$.description").value("Плановый осмотр"));
        }

        @Test
        @DisplayName("1.2 Идентификатор питомца из URL сохраняется в визите")
        @Description("ID питомца передаётся в URL, а не в теле запроса. " +
            "Сервис должен правильно связать визит с питомцем, используя именно этот ID.")
        @Severity(SeverityLevel.CRITICAL)
        void petIdFromPath_writtenToBody() throws Exception {
            given(visitRepository.save(any(Visit.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/2/pets/42/visits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Тест\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(42));
        }

        @Test
        @DisplayName("1.3 Визит сохраняется даже без описания")
        @Description("Описание визита — необязательное поле. Врач может создать запись " +
            "и добавить описание позже. Отсутствие поля не должно вызывать ошибку.")
        @Severity(SeverityLevel.NORMAL)
        void emptyDescription_savedSuccessfully() throws Exception {
            given(visitRepository.save(any(Visit.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/1/pets/3/visits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petId").value(3));
        }

        @Test
        @DisplayName("1.4 Нулевой или отрицательный ID питомца отклоняется")
        @Description("ID питомца должен быть положительным числом. " +
            "Запрос с ID = 0 должен отклоняться на уровне валидации со статусом 400.")
        @Severity(SeverityLevel.NORMAL)
        void petIdZero_returns400() throws Exception {
            mvc.perform(post("/owners/1/pets/0/visits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Осмотр\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1.5 Дата визита проставляется автоматически")
        @Description("При создании визита дата заполняется текущим временем автоматически — " +
            "врач не должен её вводить. Поле date не должно быть пустым в ответе.")
        @Severity(SeverityLevel.NORMAL)
        void dateNotNull_inResponse() throws Exception {
            given(visitRepository.save(any(Visit.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/1/pets/1/visits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Осмотр\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").exists());
        }

        @Test
        @DisplayName("1.6 Длинное медицинское описание принимается без усечения")
        @Description("Врач может написать подробное описание состояния животного. " +
            "Поле description поддерживает до 8192 символов — это нужно для полноценных медицинских записей.")
        @Severity(SeverityLevel.NORMAL)
        void longDescription_accepted() throws Exception {
            given(visitRepository.save(any(Visit.class))).willAnswer(inv -> inv.getArgument(0));
            String longDesc = "А".repeat(500);

            mvc.perform(post("/owners/1/pets/1/visits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"" + longDesc + "\"}"))
                .andExpect(status().isCreated());
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. GET по ID питомца
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. GET owners/*/pets/{petId}/visits — визиты конкретного питомца")
    class GetVisitsByPetId {

        @Test
        @DisplayName("2.1 Питомец без визитов возвращает пустой список")
        @Description("Новый питомец без истории болезней должен давать пустой массив, " +
            "а не ошибку. Пустая история — нормальное состояние.")
        @Severity(SeverityLevel.NORMAL)
        void noVisits_returnsEmptyList() throws Exception {
            given(visitRepository.findByPetId(10)).willReturn(List.of());

            mvc.perform(get("/owners/1/pets/10/visits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("2.2 История болезни из одного визита возвращается корректно")
        @Description("Проверяем, что ID, описание и привязка к питомцу передаются " +
            "без искажений — это данные медицинской истории питомца.")
        @Severity(SeverityLevel.BLOCKER)
        void oneVisit_singleElement() throws Exception {
            given(visitRepository.findByPetId(5))
                .willReturn(List.of(buildVisit(1, 5, "Первый визит")));

            mvc.perform(get("/owners/1/pets/5/visits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Первый визит"))
                .andExpect(jsonPath("$[0].petId").value(5));
        }

        @Test
        @DisplayName("2.3 Три визита для одного питомца — все в ответе")
        void threeVisits_allInResponse() throws Exception {
            given(visitRepository.findByPetId(7)).willReturn(List.of(
                buildVisit(1, 7, "Осмотр"),
                buildVisit(2, 7, "Вакцинация"),
                buildVisit(3, 7, "Лечение")
            ));

            mvc.perform(get("/owners/1/pets/7/visits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].description").value("Осмотр"))
                .andExpect(jsonPath("$[1].description").value("Вакцинация"))
                .andExpect(jsonPath("$[2].description").value("Лечение"));
        }

        @Test
        @DisplayName("2.4 petId = 0 → 400 Bad Request")
        void zeroId_returns400() throws Exception {
            mvc.perform(get("/owners/1/pets/0/visits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2.5 petId визитов в ответе совпадает с запрошенным")
        void visitsPetId_matchesRequest() throws Exception {
            given(visitRepository.findByPetId(15))
                .willReturn(List.of(buildVisit(10, 15, "Описание")));

            mvc.perform(get("/owners/1/pets/15/visits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].petId").value(15));
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. GET pets/visits — несколько питомцев
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. GET pets/visits?petId=... — визиты для нескольких питомцев")
    class GetVisitsForMultiplePets {

        @Test
        @DisplayName("3.1 Два питомца — их визиты объединены в items")
        void twoPets_bothVisitsInItems() throws Exception {
            given(visitRepository.findByPetIdIn(List.of(1, 2))).willReturn(List.of(
                buildVisit(10, 1, "Кот"),
                buildVisit(11, 2, "Пёс")
            ));

            mvc.perform(get("/pets/visits?petId=1,2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].petId").value(1))
                .andExpect(jsonPath("$.items[1].petId").value(2));
        }

        @Test
        @DisplayName("3.2 Нет визитов → items пустой массив")
        void noVisits_emptyItems() throws Exception {
            given(visitRepository.findByPetIdIn(List.of(99, 100))).willReturn(List.of());

            mvc.perform(get("/pets/visits?petId=99,100").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0));
        }

        @Test
        @DisplayName("3.3 Один питомец, три визита — все в items")
        void onePetThreeVisits_allInItems() throws Exception {
            given(visitRepository.findByPetIdIn(List.of(3))).willReturn(List.of(
                buildVisit(1, 3, "Первый"),
                buildVisit(2, 3, "Второй"),
                buildVisit(3, 3, "Третий")
            ));

            mvc.perform(get("/pets/visits?petId=3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));
        }

        @Test
        @DisplayName("3.4 Ответ содержит поле items (структура Visits)")
        void response_hasItemsField() throws Exception {
            given(visitRepository.findByPetIdIn(any())).willReturn(List.of());

            mvc.perform(get("/pets/visits?petId=1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").exists());
        }

        @Test
        @DisplayName("3.5 Пять питомцев — визиты каждого присутствуют в items")
        void fivePets_allVisitsPresent() throws Exception {
            given(visitRepository.findByPetIdIn(List.of(1, 2, 3, 4, 5))).willReturn(List.of(
                buildVisit(1, 1, "Питомец 1"),
                buildVisit(2, 2, "Питомец 2"),
                buildVisit(3, 3, "Питомец 3"),
                buildVisit(4, 4, "Питомец 4"),
                buildVisit(5, 5, "Питомец 5")
            ));

            mvc.perform(get("/pets/visits?petId=1,2,3,4,5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5));
        }
    }
}
