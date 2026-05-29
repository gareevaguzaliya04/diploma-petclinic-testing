package org.springframework.samples.petclinic.customers.unit;

import org.junit.jupiter.api.*;
import org.springframework.samples.petclinic.customers.model.*;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;


class PetServiceTest {

    // ──────────────────────────────────────────────────────
    // Группа 1: Модель Pet — базовые свойства
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Модель Pet — базовые свойства")
    class PetBasicTests {

        @Test
        @DisplayName("1.1 Новый питомец не имеет ID до сохранения в БД")
        void newPetHasNoId() {
            Pet pet = new Pet();
            assertThat(pet.getId()).isNull();
        }

        @Test
        @DisplayName("1.2 Имя питомца сохраняется и читается корректно")
        void petNameSetAndGet() {
            Pet pet = new Pet();
            pet.setName("Buddy");
            assertThat(pet.getName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("1.3 Имя питомца можно изменить")
        void petNameCanBeChanged() {
            Pet pet = new Pet();
            pet.setName("OldName");
            pet.setName("NewName");
            assertThat(pet.getName()).isEqualTo("NewName");
        }

        @Test
        @DisplayName("1.4 У нового питомца нет даты рождения")
        void newPetHasNoBirthDate() {
            assertThat(new Pet().getBirthDate()).isNull();
        }

        @Test
        @DisplayName("1.5 Дата рождения сохраняется корректно")
        void petBirthDateSetAndGet() {
            Pet pet = new Pet();
            Date date = new Date(1000000000L);
            pet.setBirthDate(date);
            assertThat(pet.getBirthDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("1.6 У нового питомца нет типа")
        void newPetHasNoType() {
            assertThat(new Pet().getType()).isNull();
        }

        @Test
        @DisplayName("1.7 Тип питомца сохраняется и читается")
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
        void petTypeNameSetAndGet() {
            PetType type = new PetType();
            type.setName("rabbit");
            assertThat(type.getName()).isEqualTo("rabbit");
        }

        @Test
        @DisplayName("2.2 Два разных типа имеют разные имена")
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
        void newOwnerHasNoId() {
            assertThat(new Owner().getId()).isNull();
        }

        @Test
        @DisplayName("3.2 Имя владельца сохраняется и читается")
        void ownerFirstNameSetAndGet() {
            Owner owner = new Owner();
            owner.setFirstName("Гузалия");
            assertThat(owner.getFirstName()).isEqualTo("Гузалия");
        }

        @Test
        @DisplayName("3.3 Фамилия владельца сохраняется и читается")
        void ownerLastNameSetAndGet() {
            Owner owner = new Owner();
            owner.setLastName("Гареева");
            assertThat(owner.getLastName()).isEqualTo("Гареева");
        }

        @Test
        @DisplayName("3.4 Адрес владельца сохраняется")
        void ownerAddressSetAndGet() {
            Owner owner = new Owner();
            owner.setAddress("ул. Ленина, д. 1");
            assertThat(owner.getAddress()).isEqualTo("ул. Ленина, д. 1");
        }

        @Test
        @DisplayName("3.5 Город владельца сохраняется")
        void ownerCitySetAndGet() {
            Owner owner = new Owner();
            owner.setCity("Уфа");
            assertThat(owner.getCity()).isEqualTo("Уфа");
        }

        @Test
        @DisplayName("3.6 Телефон владельца сохраняется")
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
        void newOwnerHasNoPets() {
            assertThat(new Owner().getPets()).isEmpty();
        }

        @Test
        @DisplayName("4.2 Один питомец добавляется к владельцу")
        void addOnePet() {
            Owner owner = new Owner();
            Pet pet = new Pet();
            pet.setName("Мурзик");
            owner.addPet(pet);
            assertThat(owner.getPets()).hasSize(1);
        }

        @Test
        @DisplayName("4.3 Два питомца добавляются к одному владельцу")
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
        void addedPetKeepsName() {
            Owner owner = new Owner();
            Pet pet = new Pet();
            pet.setName("Барсик");
            owner.addPet(pet);
            assertThat(owner.getPets().get(0).getName()).isEqualTo("Барсик");
        }

        @Test
        @DisplayName("4.5 Имя и фамилия владельца независимы друг от друга")
        void firstAndLastNameAreIndependent() {
            Owner owner = new Owner();
            owner.setFirstName("Иван");
            owner.setLastName("Петров");
            assertThat(owner.getFirstName()).isEqualTo("Иван");
            assertThat(owner.getLastName()).isEqualTo("Петров");
        }
    }
}
