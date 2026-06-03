package org.springframework.samples.petclinic.vets.web;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.vets.model.Specialty;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Контроллерное тестирование — vets-service")
@Story("REST API справочника ветеринаров")
@WebMvcTest(VetResource.class)
@ActiveProfiles("test")
class ExtendedVetResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VetRepository vetRepository;

    private Vet buildVet(int id, String firstName, String lastName) {
        Vet vet = new Vet();
        vet.setId(id);
        vet.setFirstName(firstName);
        vet.setLastName(lastName);
        return vet;
    }

    private Specialty buildSpecialty(String name) {
        Specialty s = new Specialty();
        s.setName(name);
        return s;
    }

    // ──────────────────────────────────────────────────────
    // 1. Список ветеринаров — базовые сценарии
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. GET /vets — базовые сценарии")
    class BasicVetList {

        @Test
        @DisplayName("1.1 Если ветеринаров нет, возвращается пустой список")
        @Description("При пустой базе сервис должен вернуть пустой массив, а не ошибку. " +
            "Клиент отобразит пустой список, не ломая интерфейс.")
        @Severity(SeverityLevel.NORMAL)
        void noVets_returnsEmptyList() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of());

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("1.2 Один ветеринар в базе — возвращается с корректными данными")
        @Description("Имя, фамилия и ID ветеринара должны передаваться точно. " +
            "Это базовый сценарий отображения врача в интерфейсе клиники.")
        @Severity(SeverityLevel.BLOCKER)
        void oneVet_returnsSingle() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, "Иван", "Сидоров")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Иван"))
                .andExpect(jsonPath("$[0].lastName").value("Сидоров"));
        }

        @Test
        @DisplayName("1.3 Все ветеринары из базы присутствуют в ответе")
        @Description("Ни один врач не должен потеряться при сериализации списка. " +
            "Проверяем порядок и имена всех трёх записей.")
        @Severity(SeverityLevel.CRITICAL)
        void threeVets_allInResponse() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(
                buildVet(1, "Анна", "Иванова"),
                buildVet(2, "Борис", "Петров"),
                buildVet(3, "Вера", "Сидорова")
            ));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("Анна"))
                .andExpect(jsonPath("$[1].firstName").value("Борис"))
                .andExpect(jsonPath("$[2].firstName").value("Вера"));
        }

        @Test
        @DisplayName("1.4 Ответ возвращается в формате JSON")
        @Description("Content-Type должен быть application/json — иначе клиент не сможет " +
            "автоматически разобрать ответ и отобразить список ветеринаров.")
        @Severity(SeverityLevel.NORMAL)
        void contentType_isJson() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of());

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("1.5 Количество ветеринаров в ответе совпадает с числом в базе")
        @Description("При пяти записях в базе ответ должен содержать ровно пять объектов — " +
            "ни больше (дубли), ни меньше (потери при сериализации).")
        @Severity(SeverityLevel.NORMAL)
        void fiveVets_exactCount() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(
                buildVet(1, "А", "1"), buildVet(2, "Б", "2"), buildVet(3, "В", "3"),
                buildVet(4, "Г", "4"), buildVet(5, "Д", "5")
            ));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. Специализации
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Специализации ветеринаров")
    class SpecialtyTests {

        @Test
        @DisplayName("2.1 Ветеринар без специализаций — поле specialties пустое")
        @Description("Терапевт без специализаций должен возвращать пустой массив specialties, " +
            "а не null или ошибку. Счётчик nrOfSpecialties при этом равен 0.")
        @Severity(SeverityLevel.NORMAL)
        void noSpecialties_emptyArray() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, "Терапевт", "Общий")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialties").isArray())
                .andExpect(jsonPath("$[0].specialties.length()").value(0))
                .andExpect(jsonPath("$[0].nrOfSpecialties").value(0));
        }

        @Test
        @DisplayName("2.2 Специализация ветеринара отображается в ответе")
        @Description("Если у врача есть специализация (например, хирургия), " +
            "она должна присутствовать в массиве specialties с корректным названием.")
        @Severity(SeverityLevel.CRITICAL)
        void oneSpecialty_inResponse() throws Exception {
            Vet vet = buildVet(1, "Хирург", "Иванов");
            vet.addSpecialty(buildSpecialty("surgery"));
            given(vetRepository.findAll()).willReturn(List.of(vet));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialties.length()").value(1))
                .andExpect(jsonPath("$[0].specialties[0].name").value("surgery"))
                .andExpect(jsonPath("$[0].nrOfSpecialties").value(1));
        }

        @Test
        @DisplayName("2.3 Специализации возвращаются в алфавитном порядке")
        @Description("Dentistry должна стоять перед surgery в алфавитном порядке. " +
            "Предсказуемый порядок важен для стабильного отображения в интерфейсе.")
        @Severity(SeverityLevel.NORMAL)
        void twoSpecialties_sortedAlphabetically() throws Exception {
            Vet vet = buildVet(1, "Мульти", "Специалист");
            vet.addSpecialty(buildSpecialty("surgery"));
            vet.addSpecialty(buildSpecialty("dentistry"));
            given(vetRepository.findAll()).willReturn(List.of(vet));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nrOfSpecialties").value(2))
                .andExpect(jsonPath("$[0].specialties[0].name").value("dentistry"))
                .andExpect(jsonPath("$[0].specialties[1].name").value("surgery"));
        }

        @Test
        @DisplayName("2.4 Специализации каждого ветеринара отображаются независимо")
        @Description("У специалиста должна быть его специализация, у терапевта — пустой массив. " +
            "Данные одного врача не должны смешиваться с данными другого.")
        @Severity(SeverityLevel.CRITICAL)
        void mixedVets_specialtiesAssignedCorrectly() throws Exception {
            Vet withSpec = buildVet(1, "Специалист", "Смирнов");
            withSpec.addSpecialty(buildSpecialty("radiology"));
            Vet withoutSpec = buildVet(2, "Терапевт", "Козлов");
            given(vetRepository.findAll()).willReturn(List.of(withSpec, withoutSpec));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nrOfSpecialties").value(1))
                .andExpect(jsonPath("$[1].nrOfSpecialties").value(0));
        }

        @Test
        @DisplayName("2.5 Счётчик специализаций совпадает с фактическим количеством")
        @Description("Поле nrOfSpecialties должно точно отражать количество специализаций. " +
            "Расхождение между счётчиком и реальным массивом вводит пользователя в заблуждение.")
        @Severity(SeverityLevel.NORMAL)
        void threeSpecialties_allPresentInResponse() throws Exception {
            Vet vet = buildVet(1, "Мастер", "Специальностей");
            vet.addSpecialty(buildSpecialty("surgery"));
            vet.addSpecialty(buildSpecialty("dentistry"));
            vet.addSpecialty(buildSpecialty("radiology"));
            given(vetRepository.findAll()).willReturn(List.of(vet));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nrOfSpecialties").value(3))
                .andExpect(jsonPath("$[0].specialties.length()").value(3));
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. Структура ответа
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Структура ответа")
    class ResponseStructure {

        @Test
        @DisplayName("3.1 Карточка ветеринара содержит все обязательные поля")
        @Description("Ответ должен включать ID, имя, фамилию, список специализаций и их счётчик. " +
            "Отсутствие любого поля нарушит отображение профиля врача в интерфейсе.")
        @Severity(SeverityLevel.CRITICAL)
        void allExpectedFields_present() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(42, "Доктор", "Айболит")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].firstName").exists())
                .andExpect(jsonPath("$[0].lastName").exists())
                .andExpect(jsonPath("$[0].specialties").exists())
                .andExpect(jsonPath("$[0].nrOfSpecialties").exists());
        }

        @ParameterizedTest
        @DisplayName("3.2 Имена на разных языках передаются без искажений")
        @Description("Кириллица, латиница и иероглифы должны сохраняться корректно — " +
            "клиника может обслуживать международных специалистов.")
        @Severity(SeverityLevel.NORMAL)
        @ValueSource(strings = {"Иван", "Maria", "Акэмаки", "Nguyen"})
        void variousNames_serializedCorrectly(String firstName) throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, firstName, "Тест")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value(firstName));
        }

        @Test
        @DisplayName("3.3 Список ветеринаров возвращается как массив, а не объект")
        @Description("API должен возвращать именно массив JSON — это позволяет клиенту " +
            "корректно итерировать записи независимо от их количества.")
        @Severity(SeverityLevel.NORMAL)
        void response_isArray() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, "А", "Б")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("3.4 Идентификатор ветеринара передаётся без изменений")
        @Description("ID из базы данных должен точно совпадать с ID в ответе API. " +
            "Ошибка в идентификаторе приведёт к некорректным ссылкам на профиль врача.")
        @Severity(SeverityLevel.CRITICAL)
        void vetId_matchesExpected() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(99, "Тест", "ID")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99));
        }
    }
}
