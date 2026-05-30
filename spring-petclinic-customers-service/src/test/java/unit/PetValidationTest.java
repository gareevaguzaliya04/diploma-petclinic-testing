package org.springframework.samples.petclinic.customers.unit;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.customers.model.*;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Модульное тестирование — JUnit 5 + Mockito")
@Story("Граничные случаи валидации данных")
class PetValidationTest {

    @Nested
    @DisplayName("1. Граничные случаи имён питомцев")
    class PetNameEdgeCases {

        @Test
        @DisplayName("1.1 Имя питомца из одного символа")
        @Description("Система должна принимать имена из одного символа — минимально допустимая длина важна для корректного UX формы регистрации")
        void singleCharName() {
            Pet pet = new Pet();
            pet.setName("А");
            assertThat(pet.getName()).isEqualTo("А");
        }

        @Test
        @DisplayName("1.2 Имя питомца с пробелом")
        @Description("Двойные имена с пробелом должны поддерживаться — распространённый случай для кличек домашних животных")
        void nameWithSpace() {
            Pet pet = new Pet();
            pet.setName("Рыжий Кот");
            assertThat(pet.getName()).isEqualTo("Рыжий Кот");
        }

        @Test
        @DisplayName("1.3 Имя питомца на английском")
        @Description("Английские имена питомцев должны поддерживаться — интернационализация данных для мультиязычных пользователей")
        void englishName() {
            Pet pet = new Pet();
            pet.setName("Fluffy");
            assertThat(pet.getName()).isEqualTo("Fluffy");
        }

        @ParameterizedTest
        @DisplayName("1.4 Различные имена питомцев сохраняются корректно")
        @Description("Все распространённые имена питомцев должны сохраняться без искажений — регрессионный тест для обеспечения целостности данных")
        @ValueSource(strings = {"Мурзик", "Барсик", "Пушок", "Рекс", "Бобик"})
        void variousPetNames(String name) {
            Pet pet = new Pet();
            pet.setName(name);
            assertThat(pet.getName()).isEqualTo(name);
        }
    }

    @Nested
    @DisplayName("2. Граничные случаи дат")
    class DateEdgeCases {

        @Test
        @DisplayName("2.1 Дата в прошлом сохраняется")
        @Description("Исторические даты должны сохраняться корректно — питомец мог быть рождён много лет назад и система не должна отвергать такие данные")
        void pastDate() {
            Pet pet = new Pet();
            Date past = new Date(0L); // 1970-01-01
            pet.setBirthDate(past);
            assertThat(pet.getBirthDate()).isEqualTo(past);
        }

        @Test
        @DisplayName("2.2 Текущая дата сохраняется")
        @Description("Актуальная дата регистрации должна сохраняться — стандартный сценарий при первичной регистрации нового питомца")
        void currentDate() {
            Pet pet = new Pet();
            Date now = new Date();
            pet.setBirthDate(now);
            assertThat(pet.getBirthDate()).isNotNull();
        }

        @Test
        @DisplayName("2.3 Дата рождения можно изменить")
        @Description("При повторном вводе даты должна победить последняя — корректное поведение при редактировании формы без сохранения предыдущего значения")
        void birthDateCanBeChanged() {
            Pet pet = new Pet();
            Date first  = new Date(1000000L);
            Date second = new Date(2000000L);
            pet.setBirthDate(first);
            pet.setBirthDate(second);
            assertThat(pet.getBirthDate()).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("3. Типы питомцев")
    class PetTypeEdgeCases {

        @ParameterizedTest
        @DisplayName("3.1 Стандартные типы питомцев сохраняются")
        @Description("Все стандартные типы питомцев из справочника должны корректно присваиваться питомцу — регрессионный тест справочника типов")
        @ValueSource(strings = {"cat", "dog", "bird", "hamster", "snake", "rabbit"})
        void standardPetTypes(String typeName) {
            PetType type = new PetType();
            type.setName(typeName);
            Pet pet = new Pet();
            pet.setType(type);
            assertThat(pet.getType().getName()).isEqualTo(typeName);
        }

        @Test
        @DisplayName("3.2 Тип питомца можно заменить на другой")
        @Description("Смена типа должна работать корректно — пользователь может исправить ошибку ввода без потери остальных данных питомца")
        void replaceType() {
            Pet pet = new Pet();
            PetType cat = new PetType(); cat.setName("cat");
            PetType dog = new PetType(); dog.setName("dog");
            pet.setType(cat);
            assertThat(pet.getType().getName()).isEqualTo("cat");
            pet.setType(dog);
            assertThat(pet.getType().getName()).isEqualTo("dog");
        }
    }
}
