package org.springframework.samples.petclinic.customers.web;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PetResource.class)
@ActiveProfiles("test")
class ExtendedPetResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PetRepository petRepository;

    @MockitoBean
    OwnerRepository ownerRepository;

    private Pet buildPet(int id, String name) {
        Owner owner = new Owner();
        owner.setFirstName("Тест");
        owner.setLastName("Тестов");
        owner.setAddress("ул. Ленина, д. 1");
        owner.setCity("Уфа");
        owner.setTelephone("89001234567");

        Pet pet = new Pet();
        pet.setId(id);
        pet.setName(name);

        PetType type = new PetType();
        type.setId(1);
        type.setName("cat");
        pet.setType(type);

        owner.addPet(pet);
        return pet;
    }

    private PetType buildPetType(int id, String name) {
        PetType type = new PetType();
        type.setId(id);
        type.setName(name);
        return type;
    }

    private Owner buildOwner(String first, String last) {
        Owner owner = new Owner();
        owner.setFirstName(first);
        owner.setLastName(last);
        owner.setAddress("ул. Ленина, д. 1");
        owner.setCity("Уфа");
        owner.setTelephone("89001234567");
        return owner;
    }

    // ──────────────────────────────────────────────────────
    // 1. GET /petTypes — справочник типов питомцев
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. GET /petTypes — справочник типов питомцев")
    class GetPetTypes {

        @Test
        @DisplayName("1.1 Пустой справочник → 200 с пустым массивом")
        void emptyTypes_returnsEmptyArray() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of());

            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("1.2 Один тип → 200 с одним элементом")
        void oneType_singleElement() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of(buildPetType(1, "cat")));

            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("cat"));
        }

        @Test
        @DisplayName("1.3 Шесть стандартных типов → все шесть в ответе")
        void sixStandardTypes_allInResponse() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of(
                buildPetType(1, "bird"),
                buildPetType(2, "cat"),
                buildPetType(3, "dog"),
                buildPetType(4, "hamster"),
                buildPetType(5, "rabbit"),
                buildPetType(6, "snake")
            ));

            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("bird"))
                .andExpect(jsonPath("$[2].name").value("dog"));
        }

        @Test
        @DisplayName("1.4 Ответ является массивом JSON")
        void response_isArray() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of());

            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }

        @ParameterizedTest
        @DisplayName("1.5 Различные стандартные типы корректно сериализуются")
        @ValueSource(strings = {"cat", "dog", "bird", "hamster", "rabbit", "snake"})
        void standardType_serializedCorrectly(String typeName) throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of(buildPetType(1, typeName)));

            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(typeName));
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. POST /owners/{ownerId}/pets — создание питомца
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. POST /owners/{ownerId}/pets — создание питомца")
    class CreatePet {

        @Test
        @DisplayName("2.1 Валидный запрос → 201 Created с именем питомца")
        void valid_returns201WithPetName() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Иван", "Петров")));
            given(petRepository.findPetTypeById(1)).willReturn(Optional.of(buildPetType(1, "cat")));
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Мурзик\",\"typeId\":1,\"birthDate\":\"2020-01-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Мурзик"));
        }

        @Test
        @DisplayName("2.2 Тип питомца из запроса попадает в ответ")
        void petType_writtenToResponse() throws Exception {
            given(ownerRepository.findById(2)).willReturn(Optional.of(buildOwner("Мария", "Иванова")));
            given(petRepository.findPetTypeById(3)).willReturn(Optional.of(buildPetType(3, "dog")));
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/2/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Рекс\",\"typeId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type.name").value("dog"));
        }

        @Test
        @DisplayName("2.3 Владелец не найден → 404 Not Found")
        void ownerNotFound_returns404() throws Exception {
            given(ownerRepository.findById(999)).willReturn(Optional.empty());

            mvc.perform(post("/owners/999/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Кот\",\"typeId\":1}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("2.4 ownerId = 0 нарушает @Min(1) → 400 Bad Request")
        void ownerIdZero_returns400() throws Exception {
            mvc.perform(post("/owners/0/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Кот\",\"typeId\":1}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2.5 petRepository.save() вызывается при создании")
        void valid_saveCalledOnce() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Тест", "Тестов")));
            given(petRepository.findPetTypeById(anyInt())).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Барсик\",\"typeId\":1}"))
                .andExpect(status().isCreated());

            verify(petRepository).save(any(Pet.class));
        }

        @Test
        @DisplayName("2.6 Питомец без typeId — тип не задаётся (пустой typeId = 0)")
        void petWithoutType_savedSuccessfully() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Иван", "Сидоров")));
            given(petRepository.findPetTypeById(0)).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(post("/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Пушок\",\"typeId\":0}"))
                .andExpect(status().isCreated());
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. PUT /owners/*/pets/{petId} — обновление питомца
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. PUT /owners/*/pets/{petId} — обновление питомца")
    class UpdatePet {

        @Test
        @DisplayName("3.1 Существующий питомец → 204 No Content")
        void existing_returns204() throws Exception {
            given(petRepository.findById(5)).willReturn(Optional.of(buildPet(5, "Старое имя")));
            given(petRepository.findPetTypeById(anyInt())).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(put("/owners/1/pets/5")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":5,\"name\":\"Новое имя\",\"typeId\":1}"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("3.2 Питомец не найден → 404 Not Found")
        void missing_returns404() throws Exception {
            given(petRepository.findById(999)).willReturn(Optional.empty());

            mvc.perform(put("/owners/1/pets/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":999,\"name\":\"Тест\",\"typeId\":1}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("3.3 После обновления petRepository.save() вызывается")
        void valid_saveCalledAfterUpdate() throws Exception {
            given(petRepository.findById(3)).willReturn(Optional.of(buildPet(3, "Пёс")));
            given(petRepository.findPetTypeById(anyInt())).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(put("/owners/1/pets/3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":3,\"name\":\"Рекс\",\"typeId\":2}"))
                .andExpect(status().isNoContent());

            verify(petRepository).save(any(Pet.class));
        }

        @Test
        @DisplayName("3.4 Тип питомца обновляется при наличии в справочнике")
        void typeUpdated_whenFoundInRepository() throws Exception {
            given(petRepository.findById(4)).willReturn(Optional.of(buildPet(4, "Кот")));
            given(petRepository.findPetTypeById(2)).willReturn(Optional.of(buildPetType(2, "dog")));
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(put("/owners/1/pets/4")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":4,\"name\":\"Рекс\",\"typeId\":2}"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("3.5 Тело PUT-запроса содержит id питомца — маршрутизация по id из тела")
        void petIdFromBody_usedForLookup() throws Exception {
            given(petRepository.findById(7)).willReturn(Optional.of(buildPet(7, "Барсик")));
            given(petRepository.findPetTypeById(anyInt())).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

            mvc.perform(put("/owners/99/pets/7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":7,\"name\":\"Обновлённый\",\"typeId\":1}"))
                .andExpect(status().isNoContent());
        }
    }

    // ──────────────────────────────────────────────────────
    // 4. GET /owners/*/pets/{petId} — получение питомца
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. GET /owners/*/pets/{petId} — получение питомца")
    class FindPet {

        @Test
        @DisplayName("4.1 Существующий питомец → 200 с id и именем")
        void existing_returns200WithData() throws Exception {
            given(petRepository.findById(2)).willReturn(Optional.of(buildPet(2, "Барсик")));

            mvc.perform(get("/owners/1/pets/2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Барсик"));
        }

        @Test
        @DisplayName("4.2 Несуществующий питомец → 404 Not Found")
        void missing_returns404() throws Exception {
            given(petRepository.findById(999)).willReturn(Optional.empty());

            mvc.perform(get("/owners/1/pets/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("4.3 Ответ содержит имя владельца (firstName + lastName)")
        void ownerName_inResponse() throws Exception {
            given(petRepository.findById(7)).willReturn(Optional.of(buildPet(7, "Рекс")));

            mvc.perform(get("/owners/1/pets/7").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("Тест Тестов"));
        }

        @Test
        @DisplayName("4.4 Ответ содержит тип питомца")
        void petType_inResponse() throws Exception {
            given(petRepository.findById(8)).willReturn(Optional.of(buildPet(8, "Мурзик")));

            mvc.perform(get("/owners/1/pets/8").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type.name").value("cat"));
        }

        @Test
        @DisplayName("4.5 Content-Type ответа — application/json")
        void contentType_isJson() throws Exception {
            given(petRepository.findById(1)).willReturn(Optional.of(buildPet(1, "Пушок")));

            mvc.perform(get("/owners/1/pets/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("4.6 Ответ содержит поля id, name, owner, type")
        void allExpectedFields_present() throws Exception {
            given(petRepository.findById(3)).willReturn(Optional.of(buildPet(3, "Шарик")));

            mvc.perform(get("/owners/1/pets/3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.owner").exists())
                .andExpect(jsonPath("$.type").exists());
        }
    }
}
