package org.springframework.samples.petclinic.customers.unit;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.customers.model.*;
import static org.assertj.core.api.Assertions.*;


class OwnerValidationTest {

    @Nested
    @DisplayName("1. Имена владельцев — различные форматы")
    class OwnerNameFormats {

        @ParameterizedTest
        @DisplayName("1.1 Различные имена владельцев сохраняются")
        @ValueSource(strings = {"Иван", "Maria", "Гузалия", "Александра", "Дмитрий"})
        void variousFirstNames(String name) {
            Owner owner = new Owner();
            owner.setFirstName(name);
            assertThat(owner.getFirstName()).isEqualTo(name);
        }

        @ParameterizedTest
        @DisplayName("1.2 Различные фамилии владельцев сохраняются")
        @ValueSource(strings = {"Петров", "Иванов", "Гареева", "Сидорова", "Smith"})
        void variousLastNames(String lastName) {
            Owner owner = new Owner();
            owner.setLastName(lastName);
            assertThat(owner.getLastName()).isEqualTo(lastName);
        }

        @Test
        @DisplayName("1.3 Имя из одного символа")
        void singleCharFirstName() {
            Owner owner = new Owner();
            owner.setFirstName("А");
            assertThat(owner.getFirstName()).isEqualTo("А");
        }
    }

    @Nested
    @DisplayName("2. Телефонные номера")
    class PhoneNumbers {

        @ParameterizedTest
        @DisplayName("2.1 Различные форматы телефонов сохраняются")
        @ValueSource(strings = {
            "89001234567",
            "+79001234567",
            "8(900)123-45-67",
            "74951234567"
        })
        void variousPhoneFormats(String phone) {
            Owner owner = new Owner();
            owner.setTelephone(phone);
            assertThat(owner.getTelephone()).isEqualTo(phone);
        }
    }

    @Nested
    @DisplayName("3. Города")
    class Cities {

        @ParameterizedTest
        @DisplayName("3.1 Различные города сохраняются")
        @ValueSource(strings = {"Уфа", "Москва", "Казань", "Екатеринбург", "Санкт-Петербург"})
        void variousCities(String city) {
            Owner owner = new Owner();
            owner.setCity(city);
            assertThat(owner.getCity()).isEqualTo(city);
        }
    }

    @Nested
    @DisplayName("4. Список питомцев — различные сценарии")
    class PetListScenarios {

        @Test
        @DisplayName("4.1 Пять питомцев добавляются к одному владельцу")
        void fivePets() {
            Owner owner = new Owner();
            for (int i = 1; i <= 5; i++) {
                Pet pet = new Pet();
                pet.setName("Питомец " + i);
                owner.addPet(pet);
            }
            assertThat(owner.getPets()).hasSize(5);
        }

        @Test
        @DisplayName("4.2 Первый питомец в списке имеет правильное имя")
        void firstPetHasCorrectName() {
            Owner owner = new Owner();
            Pet first = new Pet(); first.setName("Первый");
            Pet second = new Pet(); second.setName("Второй");
            owner.addPet(first);
            owner.addPet(second);
            assertThat(owner.getPets().get(0).getName()).isEqualTo("Второй");
        }

        @Test
        @DisplayName("4.3 Список питомцев не null у нового владельца")
        void petsListNotNull() {
            assertThat(new Owner().getPets()).isNotNull();
        }
    }
}
