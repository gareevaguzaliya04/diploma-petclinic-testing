package org.springframework.samples.petclinic.customers.web;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OwnerResource.class)
@Import(OwnerEntityMapper.class)
@ActiveProfiles("test")
class OwnerResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OwnerRepository ownerRepository;

    private static final String VALID_JSON = """
            {
                "firstName": "Иван",
                "lastName":  "Петров",
                "address":   "ул. Ленина, д.1",
                "city":      "Уфа",
                "telephone": "89001234567"
            }
            """;

    private Owner buildOwner(String firstName, String lastName) {
        Owner o = new Owner();
        o.setFirstName(firstName);
        o.setLastName(lastName);
        o.setAddress("ул. Ленина, д.1");
        o.setCity("Уфа");
        o.setTelephone("89001234567");
        return o;
    }

    // ──────────────────────────────────────────────────────
    // 1. POST /owners
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. POST /owners — создание владельца")
    class CreateOwner {

        @Test
        @DisplayName("1.1 Валидные данные → 201 Created")
        void valid_returns201() throws Exception {
            given(ownerRepository.save(any(Owner.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"));
        }

        @Test
        @DisplayName("1.2 Ответ содержит все поля из запроса")
        void valid_allFieldsInResponse() throws Exception {
            given(ownerRepository.save(any(Owner.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.address").value("ул. Ленина, д.1"))
                .andExpect(jsonPath("$.city").value("Уфа"))
                .andExpect(jsonPath("$.telephone").value("89001234567"));
        }

        @Test
        @DisplayName("1.3 Пустое firstName → 400 Bad Request")
        void blankFirstName_returns400() throws Exception {
            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"\",\"lastName\":\"Петров\",\"address\":\"ул.1\",\"city\":\"Уфа\",\"telephone\":\"89001234567\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1.4 Пустое lastName → 400 Bad Request")
        void blankLastName_returns400() throws Exception {
            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Иван\",\"lastName\":\"\",\"address\":\"ул.1\",\"city\":\"Уфа\",\"telephone\":\"89001234567\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1.5 Пустой address → 400 Bad Request")
        void blankAddress_returns400() throws Exception {
            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Иван\",\"lastName\":\"Петров\",\"address\":\"\",\"city\":\"Уфа\",\"telephone\":\"89001234567\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1.6 Пустой city → 400 Bad Request")
        void blankCity_returns400() throws Exception {
            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Иван\",\"lastName\":\"Петров\",\"address\":\"ул.1\",\"city\":\"\",\"telephone\":\"89001234567\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1.7 Телефон с буквами (@Digits) → 400 Bad Request")
        void telephoneWithLetters_returns400() throws Exception {
            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Иван\",\"lastName\":\"Петров\",\"address\":\"ул.1\",\"city\":\"Уфа\",\"telephone\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1.8 Репозиторий вызывается один раз при создании")
        void valid_repositoryCalledOnce() throws Exception {
            given(ownerRepository.save(any(Owner.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                .andExpect(status().isCreated());

            verify(ownerRepository).save(any(Owner.class));
        }

        @ParameterizedTest
        @DisplayName("1.9 Различные валидные имена принимаются без ошибок")
        @ValueSource(strings = {"Иван", "Maria", "Дмитрий", "Anna", "Гузалия"})
        void variousFirstNames_accepted(String firstName) throws Exception {
            given(ownerRepository.save(any(Owner.class))).willAnswer(inv -> inv.getArgument(0));

            String json = String.format(
                "{\"firstName\":\"%s\",\"lastName\":\"Тест\",\"address\":\"ул.1\",\"city\":\"Уфа\",\"telephone\":\"89001234567\"}",
                firstName);

            mvc.perform(post("/owners")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value(firstName));
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. GET /owners/{id}
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. GET /owners/{id} — поиск по ID")
    class FindOwnerById {

        @Test
        @DisplayName("2.1 Существующий ID → 200 с телом")
        void existing_returns200WithBody() throws Exception {
            Owner owner = buildOwner("Иван", "Петров");
            given(ownerRepository.findById(1)).willReturn(Optional.of(owner));

            mvc.perform(get("/owners/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"));
        }

        @Test
        @DisplayName("2.2 Несуществующий ID → 404 Not Found")
        void missing_returns404() throws Exception {
            given(ownerRepository.findById(999)).willReturn(Optional.empty());

            mvc.perform(get("/owners/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("2.3 Ответ содержит все поля владельца")
        void existing_allFieldsPresent() throws Exception {
            Owner owner = buildOwner("Гузалия", "Гареева");
            given(ownerRepository.findById(5)).willReturn(Optional.of(owner));

            mvc.perform(get("/owners/5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Гузалия"))
                .andExpect(jsonPath("$.lastName").value("Гареева"))
                .andExpect(jsonPath("$.address").value("ул. Ленина, д.1"))
                .andExpect(jsonPath("$.city").value("Уфа"))
                .andExpect(jsonPath("$.telephone").value("89001234567"));
        }

        @Test
        @DisplayName("2.4 ownerId = 0 нарушает @Min(1) → 400")
        void zeroId_returns400() throws Exception {
            mvc.perform(get("/owners/0").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2.5 Content-Type ответа — application/json")
        void responseContentType_isJson() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Тест", "Тестов")));

            mvc.perform(get("/owners/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("2.6 Ответ содержит список питомцев (пустой у нового владельца)")
        void existing_petsListPresent() throws Exception {
            given(ownerRepository.findById(2)).willReturn(Optional.of(buildOwner("Тест", "Тестов")));

            mvc.perform(get("/owners/2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pets").isArray());
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. GET /owners
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. GET /owners — список всех владельцев")
    class FindAllOwners {

        @Test
        @DisplayName("3.1 Нет владельцев → 200 с []")
        void empty_returns200EmptyList() throws Exception {
            given(ownerRepository.findAll()).willReturn(List.of());

            mvc.perform(get("/owners").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("3.2 Один владелец → массив из одного")
        void oneOwner_singleElementList() throws Exception {
            given(ownerRepository.findAll()).willReturn(List.of(buildOwner("Иван", "Петров")));

            mvc.perform(get("/owners").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].firstName").value("Иван"));
        }

        @Test
        @DisplayName("3.3 Три владельца — все в ответе")
        void threeOwners_allInResponse() throws Exception {
            given(ownerRepository.findAll()).willReturn(List.of(
                buildOwner("Первый", "А"),
                buildOwner("Второй", "Б"),
                buildOwner("Третий", "В")
            ));

            mvc.perform(get("/owners").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("Первый"))
                .andExpect(jsonPath("$[1].firstName").value("Второй"))
                .andExpect(jsonPath("$[2].firstName").value("Третий"));
        }

        @Test
        @DisplayName("3.4 Репозиторий findAll() вызывается ровно один раз")
        void findAll_repositoryCalledOnce() throws Exception {
            given(ownerRepository.findAll()).willReturn(List.of());

            mvc.perform(get("/owners").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(ownerRepository).findAll();
        }

        @Test
        @DisplayName("3.5 Ответ является массивом JSON")
        void response_isJsonArray() throws Exception {
            given(ownerRepository.findAll()).willReturn(List.of(buildOwner("А", "Б")));

            mvc.perform(get("/owners").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }
    }

    // ──────────────────────────────────────────────────────
    // 4. PUT /owners/{id}
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. PUT /owners/{id} — обновление владельца")
    class UpdateOwner {

        @Test
        @DisplayName("4.1 Существующий владелец → 204 No Content")
        void existing_returns204() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Старое", "Имя")));
            given(ownerRepository.save(any(Owner.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(put("/owners/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("4.2 Несуществующий владелец → 404 Not Found")
        void missing_returns404() throws Exception {
            given(ownerRepository.findById(999)).willReturn(Optional.empty());

            mvc.perform(put("/owners/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("4.3 Пустое firstName при обновлении → 400")
        void blankFirstName_returns400() throws Exception {
            mvc.perform(put("/owners/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"\",\"lastName\":\"Петров\",\"address\":\"ул.1\",\"city\":\"Уфа\",\"telephone\":\"89001234567\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("4.4 После обновления репозиторий save() вызван")
        void valid_saveCalledOnce() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Старое", "Имя")));
            given(ownerRepository.save(any(Owner.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(put("/owners/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"firstName":"Новое","lastName":"Имя","address":"ул.2","city":"Казань","telephone":"89009876543"}
                            """))
                .andExpect(status().isNoContent());

            verify(ownerRepository).save(any(Owner.class));
        }

        @Test
        @DisplayName("4.5 ownerId = 0 → 400 Bad Request")
        void zeroId_returns400() throws Exception {
            mvc.perform(put("/owners/0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_JSON))
                .andExpect(status().isBadRequest());
        }
    }
}
