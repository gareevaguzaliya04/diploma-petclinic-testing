package org.springframework.samples.petclinic.vets.model;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Модульное тестирование — vets-service")
@Story("Бизнес-логика моделей Vet и Specialty")
class VetModelTest {

    @Nested
    @DisplayName("1. Модель Vet — базовые свойства")
    class VetBasicTests {

        @Test
        @DisplayName("1.1 Имя ветеринара сохраняется и читается")
        @Description("firstName используется для отображения ветеринара в профиле — setter/getter должны работать корректно")
        void firstNameSetAndGet() {
            Vet vet = new Vet();
            vet.setFirstName("Иван");
            assertThat(vet.getFirstName()).isEqualTo("Иван");
        }

        @Test
        @DisplayName("1.2 Фамилия ветеринара сохраняется и читается")
        @Description("lastName используется для поиска и отображения — setter/getter должны корректно работать с кириллицей")
        void lastNameSetAndGet() {
            Vet vet = new Vet();
            vet.setLastName("Петров");
            assertThat(vet.getLastName()).isEqualTo("Петров");
        }

        @Test
        @DisplayName("1.3 ID ветеринара сохраняется")
        @Description("ID ветеринара используется для навигации к профилю — setter/getter должны работать корректно")
        void idSetAndGet() {
            Vet vet = new Vet();
            vet.setId(99);
            assertThat(vet.getId()).isEqualTo(99);
        }

        @Test
        @DisplayName("1.4 Новый ветеринар имеет пустой список специализаций")
        @Description("Новый ветеринар без специализаций должен возвращать пустой список — защита от NullPointerException в контроллере")
        void newVetHasNoSpecialties() {
            assertThat(new Vet().getSpecialties()).isEmpty();
        }

        @Test
        @DisplayName("1.5 nrOfSpecialties для нового ветеринара равен 0")
        @Description("Количество специализаций у нового ветеринара должно быть 0 — корректное начальное состояние модели")
        void newVetHasZeroSpecialties() {
            assertThat(new Vet().getNrOfSpecialties()).isEqualTo(0);
        }

        @ParameterizedTest
        @DisplayName("1.6 Различные имена ветеринаров сохраняются корректно")
        @Description("Имена на разных языках должны поддерживаться — интернационализация для мультиязычной базы специалистов")
        @ValueSource(strings = {"Иван", "Maria", "Nguyen", "Акэмаки", "Александра"})
        void variousFirstNames(String name) {
            Vet vet = new Vet();
            vet.setFirstName(name);
            assertThat(vet.getFirstName()).isEqualTo(name);
        }
    }

    @Nested
    @DisplayName("2. Специализации ветеринара")
    class SpecialtyTests {

        @Test
        @DisplayName("2.1 Добавление одной специализации увеличивает счётчик")
        @Description("addSpecialty должен увеличивать nrOfSpecialties — это влияет на отображение количества специализаций в списке врачей")
        void addOneSpecialty_countIncreases() {
            Vet vet = new Vet();
            Specialty s = new Specialty();
            s.setName("surgery");
            vet.addSpecialty(s);
            assertThat(vet.getNrOfSpecialties()).isEqualTo(1);
        }

        @Test
        @DisplayName("2.2 Добавление трёх специализаций — счётчик равен 3")
        @Description("Ветеринар может иметь несколько специализаций — все добавленные специализации должны учитываться в счётчике")
        void addThreeSpecialties_countIsThree() {
            Vet vet = new Vet();
            for (String name : new String[]{"surgery", "dentistry", "radiology"}) {
                Specialty s = new Specialty();
                s.setName(name);
                vet.addSpecialty(s);
            }
            assertThat(vet.getNrOfSpecialties()).isEqualTo(3);
        }

        @Test
        @DisplayName("2.3 Специализации возвращаются отсортированными по имени")
        @Description("getSpecialties сортирует по алфавиту — dentistry должна быть раньше surgery в UI списка специалистов")
        void specialtiesSortedAlphabetically() {
            Vet vet = new Vet();
            Specialty surgery = new Specialty(); surgery.setName("surgery");
            Specialty dentistry = new Specialty(); dentistry.setName("dentistry");
            vet.addSpecialty(surgery);
            vet.addSpecialty(dentistry);

            assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
            assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("surgery");
        }

        @Test
        @DisplayName("2.4 Список специализаций неизменяемый")
        @Description("getSpecialties возвращает unmodifiableList — внешний код не должен иметь возможности изменить внутреннее состояние модели")
        void specialtiesListIsUnmodifiable() {
            Vet vet = new Vet();
            assertThatThrownBy(() -> vet.getSpecialties().add(new Specialty()))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("2.5 Специализация с именем radiology сохраняется")
        @Description("Все стандартные специализации ветеринаров должны корректно сохраняться и отображаться")
        void radiologySpecialtyAdded() {
            Vet vet = new Vet();
            Specialty radiology = new Specialty(); radiology.setName("radiology");
            vet.addSpecialty(radiology);

            assertThat(vet.getSpecialties()).hasSize(1);
            assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("radiology");
        }

        @ParameterizedTest
        @DisplayName("2.6 Стандартные специализации добавляются корректно")
        @Description("Все специализации из справочника должны корректно добавляться к ветеринару без ошибок")
        @ValueSource(strings = {"surgery", "dentistry", "radiology"})
        void standardSpecialties(String specName) {
            Vet vet = new Vet();
            Specialty s = new Specialty(); s.setName(specName);
            vet.addSpecialty(s);
            assertThat(vet.getNrOfSpecialties()).isEqualTo(1);
            assertThat(vet.getSpecialties().get(0).getName()).isEqualTo(specName);
        }
    }

    @Nested
    @DisplayName("3. Модель Specialty")
    class SpecialtyModelTests {

        @Test
        @DisplayName("3.1 Имя специализации сохраняется")
        @Description("Specialty — строковый справочник, имя должно сохраняться точно для корректного отображения в UI")
        void nameSetAndGet() {
            Specialty s = new Specialty();
            s.setName("surgery");
            assertThat(s.getName()).isEqualTo("surgery");
        }

        @Test
        @DisplayName("3.2 Новая специализация имеет null ID до сохранения в БД")
        @Description("ID специализации генерируется JPA автоматически — до persist значение должно быть null, иначе JPA попытается сделать UPDATE вместо INSERT")
        void newSpecialtyHasNullId() {
            Specialty s = new Specialty();
            assertThat(s.getId()).isNull();
        }

        @Test
        @DisplayName("3.3 Две разные специализации имеют разные имена")
        @Description("Специализации в справочнике уникальны — их имена не должны совпадать для корректного отображения фильтра")
        void twoDifferentSpecialties() {
            Specialty s1 = new Specialty(); s1.setName("surgery");
            Specialty s2 = new Specialty(); s2.setName("dentistry");
            assertThat(s1.getName()).isNotEqualTo(s2.getName());
        }
    }
}
