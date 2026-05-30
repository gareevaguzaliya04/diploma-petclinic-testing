package org.springframework.samples.petclinic.vets.web;

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
        @DisplayName("1.1 Нет ветеринаров → 200 с пустым массивом")
        void noVets_returnsEmptyList() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of());

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("1.2 Один ветеринар → 200 с одним элементом")
        void oneVet_returnsSingle() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, "Иван", "Сидоров")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Иван"))
                .andExpect(jsonPath("$[0].lastName").value("Сидоров"));
        }

        @Test
        @DisplayName("1.3 Три ветеринара — все три в ответе")
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
        @DisplayName("1.4 Ответ имеет Content-Type application/json")
        void contentType_isJson() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of());

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("1.5 Пять ветеринаров — возвращаются ровно пять")
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
        @DisplayName("2.1 Ветеринар без специализации → specialties пустой")
        void noSpecialties_emptyArray() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, "Терапевт", "Общий")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialties").isArray())
                .andExpect(jsonPath("$[0].specialties.length()").value(0))
                .andExpect(jsonPath("$[0].nrOfSpecialties").value(0));
        }

        @Test
        @DisplayName("2.2 Ветеринар с одной специализацией — она в ответе")
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
        @DisplayName("2.3 Две специализации → отсортированы по имени (dentistry < surgery)")
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
        @DisplayName("2.4 Два ветеринара: один со специализацией, другой без")
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
        @DisplayName("2.5 Три специализации → все три в ответе, nrOfSpecialties = 3")
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
        @DisplayName("3.1 Ответ содержит поля id, firstName, lastName, specialties, nrOfSpecialties")
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
        @DisplayName("3.2 Различные имена ветеринаров корректно сериализуются")
        @ValueSource(strings = {"Иван", "Maria", "Акэмаки", "Nguyen"})
        void variousNames_serializedCorrectly(String firstName) throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, firstName, "Тест")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value(firstName));
        }

        @Test
        @DisplayName("3.3 Ответ — массив JSON, а не объект")
        void response_isArray() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(1, "А", "Б")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("3.4 ID ветеринара в ответе совпадает с переданным")
        void vetId_matchesExpected() throws Exception {
            given(vetRepository.findAll()).willReturn(List.of(buildVet(99, "Тест", "ID")));

            mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99));
        }
    }
}
