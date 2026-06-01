package org.springframework.samples.petclinic.api.dto;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Epic("Стратегия тестирования микросервисов")
@Feature("Модульное тестирование — api-gateway DTO")
@Story("Корректность передачи данных между сервисами")
class DtoTest {

    // ─────────────────────────────────────────────────────────────────
    // OwnerDetails
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. OwnerDetails — контракт данных владельца")
    class OwnerDetailsTests {

        @Test
        @DisplayName("1.1 Builder создаёт объект со всеми полями")
        @Description("OwnerDetails.Builder должен корректно переносить все поля — ошибка в Builder сломает десериализацию ответа customers-service")
        void builderSetsAllFields() {
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .id(1)
                .firstName("Гузалия")
                .lastName("Гареева")
                .address("ул. Ленина, д.1")
                .city("Уфа")
                .telephone("89001234567")
                .pets(List.of())
                .build();

            assertThat(owner.id()).isEqualTo(1);
            assertThat(owner.firstName()).isEqualTo("Гузалия");
            assertThat(owner.lastName()).isEqualTo("Гареева");
            assertThat(owner.address()).isEqualTo("ул. Ленина, д.1");
            assertThat(owner.city()).isEqualTo("Уфа");
            assertThat(owner.telephone()).isEqualTo("89001234567");
            assertThat(owner.pets()).isEmpty();
        }

        @Test
        @DisplayName("1.2 getPetIds возвращает ID всех питомцев")
        @Description("getPetIds используется api-gateway для запроса визитов у visits-service — если список неполный, часть визитов не загрузится")
        void getPetIds_returnsAllPetIds() {
            PetDetails p1 = PetDetails.PetDetailsBuilder.aPetDetails().id(10).name("Мурзик").visits(new ArrayList<>()).build();
            PetDetails p2 = PetDetails.PetDetailsBuilder.aPetDetails().id(20).name("Рекс").visits(new ArrayList<>()).build();

            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(p1, p2))
                .build();

            assertThat(owner.getPetIds()).containsExactlyInAnyOrder(10, 20);
        }

        @Test
        @DisplayName("1.3 getPetIds для пустого списка питомцев — пустой список")
        @Description("Владелец без питомцев должен возвращать пустой список ID — пустой список не должен вызывать NullPointerException в запросе визитов")
        void getPetIds_emptyPets_emptyList() {
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of())
                .build();
            assertThat(owner.getPetIds()).isEmpty();
        }

        @Test
        @DisplayName("1.4 getPetIds для трёх питомцев — все три ID")
        @Description("Все ID питомцев должны попасть в запрос к visits-service — потеря хотя бы одного ID скроет визиты питомца")
        void getPetIds_threePets() {
            List<PetDetails> pets = List.of(
                PetDetails.PetDetailsBuilder.aPetDetails().id(1).visits(new ArrayList<>()).build(),
                PetDetails.PetDetailsBuilder.aPetDetails().id(2).visits(new ArrayList<>()).build(),
                PetDetails.PetDetailsBuilder.aPetDetails().id(3).visits(new ArrayList<>()).build()
            );
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails().pets(pets).build();
            assertThat(owner.getPetIds()).hasSize(3).contains(1, 2, 3);
        }

        @ParameterizedTest
        @DisplayName("1.5 Различные города владельцев сохраняются")
        @Description("Города с кириллицей и дефисом должны сохраняться без искажений — критично для корректного отображения адреса владельца")
        @ValueSource(strings = {"Уфа", "Москва", "Санкт-Петербург", "Екатеринбург"})
        void variousCities(String city) {
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .city(city).pets(List.of()).build();
            assertThat(owner.city()).isEqualTo(city);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PetDetails
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. PetDetails — контракт данных питомца")
    class PetDetailsTests {

        @Test
        @DisplayName("2.1 Builder создаёт объект со всеми полями")
        @Description("PetDetails.Builder должен корректно переносить поля — ошибка сломает отображение карточки питомца в UI")
        void builderSetsAllFields() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(5)
                .name("Мурзик")
                .birthDate("2020-01-15")
                .visits(new ArrayList<>())
                .build();

            assertThat(pet.id()).isEqualTo(5);
            assertThat(pet.name()).isEqualTo("Мурзик");
            assertThat(pet.birthDate()).isEqualTo("2020-01-15");
            assertThat(pet.visits()).isEmpty();
        }

        @Test
        @DisplayName("2.2 Питомец с null-списком визитов получает пустой список")
        @Description("Компактный конструктор PetDetails заменяет null на пустой список — защита от NullPointerException при итерации визитов")
        void nullVisitsReplacedWithEmptyList() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(1).name("Кот").visits(null).build();
            assertThat(pet.visits()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("2.3 Список визитов у питомца не null")
        @Description("visits никогда не должен быть null — итерация без null-проверки должна быть безопасной")
        void visitsListNeverNull() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(1).name("Пёс").build();
            assertThat(pet.visits()).isNotNull();
        }

        @Test
        @DisplayName("2.4 Питомец с тремя визитами — все три в списке")
        @Description("Все визиты питомца должны присутствовать в DTO — потеря визита нарушит отображение медицинской истории")
        void petWithThreeVisits() {
            List<VisitDetails> visits = List.of(
                new VisitDetails(1, 5, "2024-01-01", "Первый"),
                new VisitDetails(2, 5, "2024-02-01", "Второй"),
                new VisitDetails(3, 5, "2024-03-01", "Третий")
            );
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(5).name("Барсик").visits(visits).build();

            assertThat(pet.visits()).hasSize(3);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // VisitDetails
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. VisitDetails — контракт данных визита")
    class VisitDetailsTests {

        @Test
        @DisplayName("3.1 Все поля визита доступны через record-компоненты")
        @Description("VisitDetails — record, все поля должны быть доступны без ошибок — это публичный контракт между gateway и visits-service")
        void allFieldsAccessible() {
            VisitDetails visit = new VisitDetails(1, 10, "2024-06-15", "Осмотр");
            assertThat(visit.id()).isEqualTo(1);
            assertThat(visit.petId()).isEqualTo(10);
            assertThat(visit.date()).isEqualTo("2024-06-15");
            assertThat(visit.description()).isEqualTo("Осмотр");
        }

        @Test
        @DisplayName("3.2 Два визита с разными описаниями различаются")
        @Description("Разные визиты не должны быть идентичными — корректность разграничения записей в медицинской истории")
        void twoDifferentVisits() {
            VisitDetails v1 = new VisitDetails(1, 5, "2024-01-01", "Первый");
            VisitDetails v2 = new VisitDetails(2, 5, "2024-02-01", "Второй");
            assertThat(v1.description()).isNotEqualTo(v2.description());
        }

        @Test
        @DisplayName("3.3 petId визита совпадает с переданным")
        @Description("petId связывает визит с нужным питомцем — ошибка в этом поле приведёт к отображению визита у чужого питомца")
        void petIdMatchesExpected() {
            VisitDetails visit = new VisitDetails(1, 42, "2024-01-01", "Тест");
            assertThat(visit.petId()).isEqualTo(42);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Visits (обёртка)
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. Visits — обёртка над списком визитов")
    class VisitsWrapperTests {

        @Test
        @DisplayName("4.1 Visits с пустым списком — items пустой")
        @Description("Обёртка с пустым списком должна работать корректно — visits-service может вернуть пустой массив")
        void emptyVisits() {
            Visits visits = new Visits(List.of());
            assertThat(visits.items()).isEmpty();
        }

        @Test
        @DisplayName("4.2 Visits с двумя элементами — оба доступны")
        @Description("Все визиты из ответа visits-service должны быть доступны через items() — потеря записей недопустима")
        void twoVisitsAccessible() {
            List<VisitDetails> list = List.of(
                new VisitDetails(1, 1, "2024-01-01", "Первый"),
                new VisitDetails(2, 2, "2024-02-01", "Второй")
            );
            Visits visits = new Visits(list);
            assertThat(visits.items()).hasSize(2);
        }

        @Test
        @DisplayName("4.3 items() не null при любом состоянии")
        @Description("Метод items() никогда не должен возвращать null — защита от NullPointerException при распределении визитов по питомцам")
        void itemsNeverNull() {
            assertThat(new Visits(List.of()).items()).isNotNull();
        }
    }
}
