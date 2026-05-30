package org.springframework.samples.petclinic.customers.unit;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.customers.model.*;
import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Модульное тестирование — JUnit 5 + Mockito")
@Story("Валидация данных владельца")
class OwnerValidationTest {

    @Nested
    @DisplayName("1. Имена владельцев — различные форматы")
    class OwnerNameFormats {

        @ParameterizedTest
        @DisplayName("1.1 Различные имена владельцев сохраняются")
        @Description("Имена владельцев на разных языках должны сохраняться без искажений — поддержка Unicode необходима для интернационального использования системы")
        @ValueSource(strings = {"Иван", "Maria", "Гузалия", "Александра", "Дмитрий"})
        void variousFirstNames(String name) {
            Owner owner = new Owner();
            owner.setFirstName(name);
            assertThat(owner.getFirstName()).isEqualTo(name);
        }

        @ParameterizedTest
        @DisplayName("1.2 Различные фамилии владельцев сохраняются")
        @Description("Фамилии владельцев на разных языках должны сохраняться корректно — поддержка Unicode и кириллицы критична для российского рынка")
        @ValueSource(strings = {"Петров", "Иванов", "Гареева", "Сидорова", "Smith"})
        void variousLastNames(String lastName) {
            Owner owner = new Owner();
            owner.setLastName(lastName);
            assertThat(owner.getLastName()).isEqualTo(lastName);
        }

        @Test
        @DisplayName("1.3 Имя из одного символа")
        @Description("Граничный случай — имя из одного символа должно быть допустимым для исключения ложных ошибок валидации при вводе")
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
        @Description("Телефоны в разных российских форматах должны приниматься системой — жёсткое ограничение формата приведёт к потере части пользователей")
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
        @Description("Города с различными символами (дефис, кириллица) должны сохраняться без потерь — это влияет на фильтрацию владельцев по географии")
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
        @Description("При добавлении множества питомцев список должен расти корректно — проверяем отсутствие потерь при накоплении объектов")
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
        @Description("Порядок питомцев в списке должен быть предсказуем — от этого зависит корректное отображение питомцев в профиле владельца")
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
        @Description("Список питомцев не должен быть null у нового владельца — защита от NullPointerException при обращении к пустому списку в контроллерах")
        void petsListNotNull() {
            assertThat(new Owner().getPets()).isNotNull();
        }
    }
}
