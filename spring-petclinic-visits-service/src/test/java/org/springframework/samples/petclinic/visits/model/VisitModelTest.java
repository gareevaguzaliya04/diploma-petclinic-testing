package org.springframework.samples.petclinic.visits.model;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Модульное тестирование — visits-service")
@Story("Бизнес-логика модели Visit")
class VisitModelTest {

    @Nested
    @DisplayName("1. Модель Visit — базовые свойства")
    class VisitBasicTests {

        @Test
        @DisplayName("1.1 Новый визит имеет дату по умолчанию")
        @Description("При создании нового визита дата заполняется автоматически текущим временем — врач не обязан вводить дату вручную")
        void newVisitHasDefaultDate() {
            Visit visit = new Visit();
            assertThat(visit.getDate()).isNotNull();
        }

        @Test
        @DisplayName("1.2 Описание визита сохраняется корректно")
        @Description("Медицинское описание визита должно сохраняться без искажений — потеря данных недопустима для медицинской истории")
        void descriptionSetAndGet() {
            Visit visit = new Visit();
            visit.setDescription("Плановый осмотр");
            assertThat(visit.getDescription()).isEqualTo("Плановый осмотр");
        }

        @Test
        @DisplayName("1.3 ID питомца сохраняется и читается")
        @Description("petId связывает визит с питомцем — ошибка в этом поле приведёт к привязке визита к чужому питомцу")
        void petIdSetAndGet() {
            Visit visit = new Visit();
            visit.setPetId(42);
            assertThat(visit.getPetId()).isEqualTo(42);
        }

        @Test
        @DisplayName("1.4 ID визита сохраняется и читается")
        @Description("ID визита используется для навигации и ссылок — setter/getter должны работать корректно")
        void idSetAndGet() {
            Visit visit = new Visit();
            visit.setId(7);
            assertThat(visit.getId()).isEqualTo(7);
        }

        @Test
        @DisplayName("1.5 Дату визита можно изменить")
        @Description("Дату визита можно скорректировать — нужно для редактирования записи врачом")
        void dateCanBeChanged() {
            Visit visit = new Visit();
            Date newDate = new Date(2000000000L);
            visit.setDate(newDate);
            assertThat(visit.getDate()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("1.6 Новый визит не имеет ID до сохранения в БД")
        @Description("До сохранения в репозитории ID должен быть null — гарантирует корректный INSERT без конфликта первичного ключа")
        void newVisitHasNoId() {
            assertThat(new Visit().getId()).isNull();
        }

        @Test
        @DisplayName("1.7 Описание визита можно обновить")
        @Description("Врач должен иметь возможность исправить описание — setter должен перезаписывать предыдущее значение")
        void descriptionCanBeUpdated() {
            Visit visit = new Visit();
            visit.setDescription("Первичный");
            visit.setDescription("Повторный");
            assertThat(visit.getDescription()).isEqualTo("Повторный");
        }
    }

    @Nested
    @DisplayName("2. Граничные случаи описания")
    class DescriptionEdgeCases {

        @ParameterizedTest
        @DisplayName("2.1 Различные медицинские описания сохраняются")
        @Description("Все типы медицинских записей должны сохраняться точно — регрессионный тест для различных форматов текста")
        @ValueSource(strings = {
            "Плановый осмотр",
            "Вакцинация от бешенства",
            "Хирургическое вмешательство",
            "Annual checkup",
            "Консультация специалиста"
        })
        void variousDescriptions(String desc) {
            Visit visit = new Visit();
            visit.setDescription(desc);
            assertThat(visit.getDescription()).isEqualTo(desc);
        }

        @Test
        @DisplayName("2.2 Длинное описание (500 символов) принимается")
        @Description("Поле description допускает до 8192 символов по аннотации @Size — длинные медицинские записи должны сохраняться")
        void longDescriptionAccepted() {
            Visit visit = new Visit();
            String longDesc = "А".repeat(500);
            visit.setDescription(longDesc);
            assertThat(visit.getDescription()).hasSize(500);
        }

        @Test
        @DisplayName("2.3 Описание null допускается")
        @Description("Описание визита не является обязательным полем — пустая запись допустима при первичном создании")
        void nullDescriptionAllowed() {
            Visit visit = new Visit();
            visit.setDescription(null);
            assertThat(visit.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("3. Builder — корректность построения объекта")
    class VisitBuilderTests {

        @Test
        @DisplayName("3.1 Builder создаёт объект с заданными полями")
        @Description("Builder должен корректно переносить все поля в созданный объект — иначе данные теряются при построении")
        void builderCreatesCorrectObject() {
            Date date = new Date(1000000L);
            Visit visit = Visit.VisitBuilder.aVisit()
                .id(1)
                .petId(10)
                .date(date)
                .description("Осмотр")
                .build();

            assertThat(visit.getId()).isEqualTo(1);
            assertThat(visit.getPetId()).isEqualTo(10);
            assertThat(visit.getDate()).isEqualTo(date);
            assertThat(visit.getDescription()).isEqualTo("Осмотр");
        }

        @Test
        @DisplayName("3.2 Два Builder-вызова дают независимые объекты")
        @Description("Объекты, созданные через Builder, не должны делить состояние — изменение одного не затрагивает другой")
        void twoBuilderCallsAreIndependent() {
            Visit v1 = Visit.VisitBuilder.aVisit().petId(1).description("Первый").build();
            Visit v2 = Visit.VisitBuilder.aVisit().petId(2).description("Второй").build();

            assertThat(v1.getPetId()).isEqualTo(1);
            assertThat(v2.getPetId()).isEqualTo(2);
            assertThat(v1.getDescription()).isNotEqualTo(v2.getDescription());
        }

        @Test
        @DisplayName("3.3 Builder без описания — поле description null")
        @Description("Если описание не задано в Builder, объект создаётся с null-описанием — это допустимое состояние")
        void builderWithoutDescription() {
            Visit visit = Visit.VisitBuilder.aVisit().petId(5).build();
            assertThat(visit.getDescription()).isNull();
        }
    }
}
