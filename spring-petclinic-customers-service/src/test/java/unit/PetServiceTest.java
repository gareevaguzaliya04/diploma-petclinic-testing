package org.springframework.samples.petclinic.customers.unit;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.springframework.samples.petclinic.customers.model.*;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Модульное тестирование — JUnit 5 + Mockito")
@Story("Бизнес-логика модели Pet и Owner")
class PetServiceTest {

    // ──────────────────────────────────────────────────────
    // Группа 1: Модель Pet — базовые свойства
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Модель Pet — базовые свойства")
    class PetBasicTests {

        @Test
        @DisplayName("1.1 Новый питомец не имеет ID до сохранения в БД")
        @Description("Убеждаемся что новый объект Pet не имеет ID до сохранения в репозитории — гарантирует корректное поведение JPA при INSERT без конфликта первичного ключа")
        void newPetHasNoId() {
            Pet pet = new Pet();
            assertThat(pet.getId()).isNull();
        }

        @Test
        @DisplayName("1.2 Имя питомца сохраняется и читается корректно")
        @Description("Проверяем что setter/getter для поля name работают корректно — основа для отображения имени питомца в UI клиники")
        void petNameSetAndGet() {
            Pet pet = new Pet();
            pet.setName("Buddy");
            assertThat(pet.getName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("1.3 Имя питомца можно изменить")
        @Description("Убеждаемся что имя можно перезаписать — корректное поведение при редактировании карточки питомца пользователем")
        void petNameCanBeChanged() {
            Pet pet = new Pet();
            pet.setName("OldName");
            pet.setName("NewName");
            assertThat(pet.getName()).isEqualTo("NewName");
        }

        @Test
        @DisplayName("1.4 У нового питомца нет даты рождения")
        @Description("Новый питомец не должен иметь дату рождения по умолчанию — поле опциональное при первичной регистрации")
        void newPetHasNoBirthDate() {
            assertThat(new Pet().getBirthDate()).isNull();
        }

        @Test
        @DisplayName("1.5 Дата рождения сохраняется корректно")
        @Description("Дата рождения должна сохраняться без изменений — критично для корректного отображения возраста питомца в профиле")
        void petBirthDateSetAndGet() {
            Pet pet = new Pet();
            Date date = new Date(1000000000L);
            pet.setBirthDate(date);
            assertThat(pet.getBirthDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("1.6 У нового питомца нет типа")
        @Description("Новый питомец не должен иметь тип по умолчанию — пользователь обязан выбрать тип при регистрации через форму")
        void newPetHasNoType() {
            assertThat(new Pet().getType()).isNull();
        }

        @Test
        @DisplayName("1.7 Тип питомца сохраняется и читается")
        @Description("Тип питомца отображается в профиле и используется для фильтрации — setter/getter должны работать корректно")
        void petTypeSetAndGet() {
            Pet pet = new Pet();
            PetType type = new PetType();
            type.setName("cat");
            pet.setType(type);
            assertThat(pet.getType()).isNotNull();
            assertThat(pet.getType().getName()).isEqualTo("cat");
        }

        @Test
        @DisplayName("1.8 Тип питомца можно изменить")
        @Description("При смене типа новый тип должен заменить старый — проверяем отсутствие кэширования устаревшего значения")
        void petTypeCanBeChanged() {
            Pet pet = new Pet();
            PetType cat = new PetType(); cat.setName("cat");
            PetType dog = new PetType(); dog.setName("dog");
            pet.setType(cat);
            pet.setType(dog);
            assertThat(pet.getType().getName()).isEqualTo("dog");
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 2: Модель PetType
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Модель PetType")
    class PetTypeTests {

        @Test
        @DisplayName("2.1 Имя типа питомца сохраняется")
        @Description("Тип питомца — строковый справочник — имя должно сохраняться точно для корректного отображения в выпадающих списках UI")
        void petTypeNameSetAndGet() {
            PetType type = new PetType();
            type.setName("rabbit");
            assertThat(type.getName()).isEqualTo("rabbit");
        }

        @Test
        @DisplayName("2.2 Два разных типа имеют разные имена")
        @Description("Два типа питомцев не должны быть равны — нужно для корректного отображения справочника и исключения дубликатов")
        void twoDifferentTypes() {
            PetType cat = new PetType(); cat.setName("cat");
            PetType dog = new PetType(); dog.setName("dog");
            assertThat(cat.getName()).isNotEqualTo(dog.getName());
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 3: Модель Owner — базовые свойства
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Модель Owner — базовые свойства")
    class OwnerBasicTests {

        @Test
        @DisplayName("3.1 Новый владелец не имеет ID")
        @Description("Новый владелец до сохранения в БД не должен иметь ID — гарантирует корректный INSERT без конфликта первичного ключа")
        void newOwnerHasNoId() {
            assertThat(new Owner().getId()).isNull();
        }

        @Test
        @DisplayName("3.2 Имя владельца сохраняется и читается")
        @Description("Имя владельца используется для отображения в профиле и поиска — setter/getter должны работать корректно с кириллицей")
        void ownerFirstNameSetAndGet() {
            Owner owner = new Owner();
            owner.setFirstName("Гузалия");
            assertThat(owner.getFirstName()).isEqualTo("Гузалия");
        }

        @Test
        @DisplayName("3.3 Фамилия владельца сохраняется и читается")
        @Description("Фамилия используется для поиска в списке владельцев — setter/getter должны сохранять значение без искажений")
        void ownerLastNameSetAndGet() {
            Owner owner = new Owner();
            owner.setLastName("Гареева");
            assertThat(owner.getLastName()).isEqualTo("Гареева");
        }

        @Test
        @DisplayName("3.4 Адрес владельца сохраняется")
        @Description("Адрес отображается в профиле владельца — setter/getter должны корректно работать со строками, содержащими знаки препинания")
        void ownerAddressSetAndGet() {
            Owner owner = new Owner();
            owner.setAddress("ул. Ленина, д. 1");
            assertThat(owner.getAddress()).isEqualTo("ул. Ленина, д. 1");
        }

        @Test
        @DisplayName("3.5 Город владельца сохраняется")
        @Description("Город используется для сортировки и фильтрации владельцев в списке — setter/getter должны работать корректно")
        void ownerCitySetAndGet() {
            Owner owner = new Owner();
            owner.setCity("Уфа");
            assertThat(owner.getCity()).isEqualTo("Уфа");
        }

        @Test
        @DisplayName("3.6 Телефон владельца сохраняется")
        @Description("Телефон используется для связи с владельцем — setter/getter должны сохранять номер без изменений формата")
        void ownerTelephoneSetAndGet() {
            Owner owner = new Owner();
            owner.setTelephone("89001234567");
            assertThat(owner.getTelephone()).isEqualTo("89001234567");
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 4: Owner — работа со списком питомцев
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. Owner — управление списком питомцев")
    class OwnerPetsTests {

        @Test
        @DisplayName("4.1 Новый владелец имеет пустой список питомцев")
        @Description("Новый владелец не должен иметь питомцев — корректное начальное состояние модели предотвращает NullPointerException")
        void newOwnerHasNoPets() {
            assertThat(new Owner().getPets()).isEmpty();
        }

        @Test
        @DisplayName("4.2 Один питомец добавляется к владельцу")
        @Description("Метод addPet должен увеличивать список питомцев владельца — базовая бизнес-операция при регистрации питомца")
        void addOnePet() {
            Owner owner = new Owner();
            Pet pet = new Pet();
            pet.setName("Мурзик");
            owner.addPet(pet);
            assertThat(owner.getPets()).hasSize(1);
        }

        @Test
        @DisplayName("4.3 Два питомца добавляются к одному владельцу")
        @Description("Несколько питомцев добавляются без перезаписи друг друга — список владельца должен корректно расти")
        void addTwoPets() {
            Owner owner = new Owner();
            Pet p1 = new Pet(); p1.setName("Кот");
            Pet p2 = new Pet(); p2.setName("Пёс");
            owner.addPet(p1);
            owner.addPet(p2);
            assertThat(owner.getPets()).hasSize(2);
        }

        @Test
        @DisplayName("4.4 Имя добавленного питомца сохраняется")
        @Description("Имя питомца не должно теряться при добавлении к владельцу — целостность данных при связывании объектов Owner и Pet")
        void addedPetKeepsName() {
            Owner owner = new Owner();
            Pet pet = new Pet();
            pet.setName("Барсик");
            owner.addPet(pet);
            assertThat(owner.getPets().get(0).getName()).isEqualTo("Барсик");
        }

        @Test
        @DisplayName("4.5 Имя и фамилия владельца независимы друг от друга")
        @Description("Изменение имени не должно влиять на фамилию — поля хранятся независимо в модели Owner")
        void firstAndLastNameAreIndependent() {
            Owner owner = new Owner();
            owner.setFirstName("Иван");
            owner.setLastName("Петров");
            assertThat(owner.getFirstName()).isEqualTo("Иван");
            assertThat(owner.getLastName()).isEqualTo("Петров");
        }
    }
}
