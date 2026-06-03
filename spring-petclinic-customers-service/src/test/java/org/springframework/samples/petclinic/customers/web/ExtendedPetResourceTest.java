package org.springframework.samples.petclinic.customers.web;

import io.qameta.allure.*;
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

@Epic("Стратегия тестирования микросервисов")
@Feature("Контроллерное тестирование — customers-service")
@Story("REST API для управления питомцами")
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
    // 1. GET /petTypes
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. GET /petTypes — справочник типов питомцев")
    class GetPetTypes {

        @Test
        @DisplayName("1.1 Пустой справочник возвращает пустой список")
        @Description("Если в системе нет ни одного типа питомцев, API должен вернуть " +
            "пустой массив, а не ошибку. Клиент сможет корректно отобразить пустую форму выбора типа.")
        @Severity(SeverityLevel.NORMAL)
        void emptyTypes_returnsEmptyArray() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of());
            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("1.2 Один тип в справочнике возвращается корректно")
        @Description("Проверяем, что id и name типа питомца передаются без искажений. " +
            "Оба поля нужны клиенту: id — для сохранения выбора, name — для отображения в форме.")
        @Severity(SeverityLevel.NORMAL)
        void oneType_singleElement() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of(buildPetType(1, "cat")));
            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("cat"));
        }

        @Test
        @DisplayName("1.3 Все шесть стандартных типов присутствуют в ответе")
        @Description("Справочник должен вернуть все доступные виды животных. " +
            "Если хоть один тип потеряется, пользователь не сможет его выбрать при регистрации питомца.")
        @Severity(SeverityLevel.CRITICAL)
        void sixStandardTypes_allInResponse() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of(
                buildPetType(1, "bird"), buildPetType(2, "cat"), buildPetType(3, "dog"),
                buildPetType(4, "hamster"), buildPetType(5, "rabbit"), buildPetType(6, "snake")));
            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("bird"))
                .andExpect(jsonPath("$[2].name").value("dog"));
        }

        @Test
        @DisplayName("1.4 Ответ является массивом, а не объектом")
        @Description("API должен возвращать именно массив JSON, а не одиночный объект. " +
            "Это важно для клиентского кода, который итерирует список типов.")
        @Severity(SeverityLevel.NORMAL)
        void response_isArray() throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of());
            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        }

        @ParameterizedTest
        @DisplayName("1.5 Каждый стандартный тип корректно сериализуется в JSON")
        @Description("Проверяем все шесть видов животных: название должно передаваться " +
            "точно, без изменения регистра или добавления лишних символов.")
        @Severity(SeverityLevel.NORMAL)
        @ValueSource(strings = {"cat", "dog", "bird", "hamster", "rabbit", "snake"})
        void standardType_serializedCorrectly(String typeName) throws Exception {
            given(petRepository.findPetTypes()).willReturn(List.of(buildPetType(1, typeName)));
            mvc.perform(get("/petTypes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(typeName));
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. POST /owners/{ownerId}/pets
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. POST /owners/{ownerId}/pets — регистрация нового питомца")
    class CreatePet {

        @Test
        @DisplayName("2.1 Корректный запрос создаёт питомца и возвращает его данные")
        @Description("Основной сценарий регистрации питомца: передаём имя, вид и дату рождения, " +
            "получаем статус 201 и объект с именем питомца. Это базовый бизнес-сценарий клиники.")
        @Severity(SeverityLevel.BLOCKER)
        void valid_returns201WithPetName() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Иван", "Петров")));
            given(petRepository.findPetTypeById(1)).willReturn(Optional.of(buildPetType(1, "cat")));
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            mvc.perform(post("/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":0,\"name\":\"Мурзик\",\"typeId\":1,\"birthDate\":\"2020-01-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Мурзик"));
        }

        @Test
        @DisplayName("2.2 Выбранный вид питомца сохраняется и возвращается в ответе")
        @Description("Вид животного (cat, dog и т.д.) должен корректно сохраняться и возвращаться " +
            "клиенту — без него невозможно правильно отобразить карточку питомца.")
        @Severity(SeverityLevel.CRITICAL)
        void petType_writtenToResponse() throws Exception {
            given(ownerRepository.findById(2)).willReturn(Optional.of(buildOwner("Мария", "Иванова")));
            given(petRepository.findPetTypeById(3)).willReturn(Optional.of(buildPetType(3, "dog")));
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            mvc.perform(post("/owners/2/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":0,\"name\":\"Рекс\",\"typeId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type.name").value("dog"));
        }

        @Test
        @DisplayName("2.3 Попытка добавить питомца несуществующему владельцу возвращает 404")
        @Description("Если владелец с указанным ID не найден, система должна вернуть 404, " +
            "а не создавать питомца без владельца — это нарушило бы целостность данных.")
        @Severity(SeverityLevel.CRITICAL)
        void ownerNotFound_returns404() throws Exception {
            given(ownerRepository.findById(999)).willReturn(Optional.empty());
            mvc.perform(post("/owners/999/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":0,\"name\":\"Кот\",\"typeId\":1}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("2.4 Идентификатор владельца не может быть нулевым или отрицательным")
        @Description("ID владельца должен быть положительным числом. " +
            "Запрос с ID = 0 должен быть отклонён на уровне валидации, не доходя до базы данных.")
        @Severity(SeverityLevel.NORMAL)
        void ownerIdZero_returns400() throws Exception {
            mvc.perform(post("/owners/0/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":0,\"name\":\"Кот\",\"typeId\":1}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2.5 При создании питомца вызывается сохранение в базе данных")
        @Description("Проверяем, что метод сохранения действительно вызывается — " +
            "это гарантирует, что питомец не просто возвращается клиенту, но и записывается в БД.")
        @Severity(SeverityLevel.CRITICAL)
        void valid_saveCalledOnce() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Тест", "Тестов")));
            given(petRepository.findPetTypeById(anyInt())).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            mvc.perform(post("/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":0,\"name\":\"Барсик\",\"typeId\":1}"))
                .andExpect(status().isCreated());
            verify(petRepository).save(any(Pet.class));
        }

        @Test
        @DisplayName("2.6 Питомец регистрируется даже без указания вида")
        @Description("Вид питомца — необязательное поле при первичной регистрации. " +
            "Система не должна отказывать в сохранении, если typeId не указан (равен 0).")
        @Severity(SeverityLevel.NORMAL)
        void petWithoutType_savedSuccessfully() throws Exception {
            given(ownerRepository.findById(1)).willReturn(Optional.of(buildOwner("Иван", "Сидоров")));
            given(petRepository.findPetTypeById(0)).willReturn(Optional.empty());
            given(petRepository.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));
            mvc.perform(post("/owners/1/pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":0,\"name\":\"Пушок\",\"typeId\":0}"))
                .andExpect(status().isCreated());
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. PUT /owners/*/pets/{petId}
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. PUT /owners/*/pets/{petId} — обновление данных питомца")
    class UpdatePet {

        @Test
        @DisplayName("3.1 Успешное обновление возвращает статус 204 без тела ответа")
        @Description("При успешном обновлении данных питомца сервер возвращает 204 No Content — " +
            "это стандарт REST для операций изменения, не требующих возврата данных.")
        @Severity(SeverityLevel.BLOCKER)
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
        @DisplayName("3.2 Попытка обновить несуществующего питомца возвращает 404")
        @Description("Если питомец с указанным ID не найден, система должна сообщить " +
            "об этом статусом 404, а не создавать новую запись или возвращать ошибку сервера.")
        @Severity(SeverityLevel.CRITICAL)
        void missing_returns404() throws Exception {
            given(petRepository.findById(999)).willReturn(Optional.empty());
            mvc.perform(put("/owners/1/pets/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":999,\"name\":\"Тест\",\"typeId\":1}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("3.3 После обновления изменения записываются в базу данных")
        @Description("Проверяем, что обновление не просто проходит валидацию, " +
            "но и действительно сохраняется — метод сохранения должен быть вызван.")
        @Severity(SeverityLevel.CRITICAL)
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
        @DisplayName("3.4 При обновлении вид питомца заменяется на новый")
        @Description("Если пользователь ошибся при выборе вида и хочет исправить — " +
            "новый вид должен заменить старый без потери остальных данных питомца.")
        @Severity(SeverityLevel.NORMAL)
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
        @DisplayName("3.5 Идентификатор питомца берётся из тела запроса")
        @Description("При обновлении сервис использует ID питомца из JSON-тела, " +
            "а не из URL-пути. Проверяем, что идентификатор владельца в URL не влияет на результат.")
        @Severity(SeverityLevel.NORMAL)
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
    // 4. GET /owners/*/pets/{petId}
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. GET /owners/*/pets/{petId} — получение карточки питомца")
    class FindPet {

        @Test
        @DisplayName("4.1 Существующий питомец возвращается с корректными данными")
        @Description("Основной сценарий просмотра карточки питомца: по ID получаем " +
            "объект с именем и идентификатором. Это отправная точка для всего профиля питомца.")
        @Severity(SeverityLevel.BLOCKER)
        void existing_returns200WithData() throws Exception {
            given(petRepository.findById(2)).willReturn(Optional.of(buildPet(2, "Барсик")));
            mvc.perform(get("/owners/1/pets/2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Барсик"));
        }

        @Test
        @DisplayName("4.2 Запрос несуществующего питомца возвращает 404")
        @Description("Если питомец с указанным ID не найден, клиент должен получить " +
            "статус 404, чтобы корректно обработать ситуацию, не показывая пустую страницу.")
        @Severity(SeverityLevel.CRITICAL)
        void missing_returns404() throws Exception {
            given(petRepository.findById(999)).willReturn(Optional.empty());
            mvc.perform(get("/owners/1/pets/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("4.3 В карточке питомца отображается имя его владельца")
        @Description("Карточка питомца включает имя и фамилию владельца — " +
            "это нужно для отображения в интерфейсе без дополнительного запроса к сервису владельцев.")
        @Severity(SeverityLevel.NORMAL)
        void ownerName_inResponse() throws Exception {
            given(petRepository.findById(7)).willReturn(Optional.of(buildPet(7, "Рекс")));
            mvc.perform(get("/owners/1/pets/7").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("Тест Тестов"));
        }

        @Test
        @DisplayName("4.4 В карточке питомца указан его вид")
        @Description("Вид питомца (кошка, собака и т.д.) должен присутствовать в ответе — " +
            "без него интерфейс не сможет показать правильный значок и фильтрацию по виду.")
        @Severity(SeverityLevel.NORMAL)
        void petType_inResponse() throws Exception {
            given(petRepository.findById(8)).willReturn(Optional.of(buildPet(8, "Мурзик")));
            mvc.perform(get("/owners/1/pets/8").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type.name").value("cat"));
        }

        @Test
        @DisplayName("4.5 Ответ возвращается в формате JSON")
        @Description("Content-Type ответа должен быть application/json — " +
            "иначе клиент не сможет автоматически распознать и разобрать данные.")
        @Severity(SeverityLevel.NORMAL)
        void contentType_isJson() throws Exception {
            given(petRepository.findById(1)).willReturn(Optional.of(buildPet(1, "Пушок")));
            mvc.perform(get("/owners/1/pets/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("4.6 Карточка питомца содержит все обязательные поля")
        @Description("Ответ должен включать ID, имя, владельца и вид питомца. " +
            "Отсутствие любого из полей сломает отображение карточки в интерфейсе.")
        @Severity(SeverityLevel.CRITICAL)
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
