package org.springframework.samples.petclinic.customers.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.samples.petclinic.customers.model.*;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

/**
 * Интеграционные тесты для OwnerRepository.
 * Уровень: INTEGRATION (требует Docker — запускает PostgreSQL 15 через Testcontainers).
 *
 * ВАЖНО: переменная TESTCONTAINERS_RYUK_DISABLED=true обязательна в CI/CD.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OwnerRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("customers_test_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Автоматически создаём схему БД при старте Spring-контекста.
        // Без этого параметра тесты падают в CI с "relation owners does not exist"
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode",           () -> "never");
    }

    @Autowired
    private OwnerRepository ownerRepository;

    // Вспомогательный метод для создания тестового владельца
    private Owner buildOwner(String firstName, String lastName) {
        Owner o = new Owner();
        o.setFirstName(firstName);
        o.setLastName(lastName);
        o.setAddress("Тестовая ул., д. 1");
        o.setCity("Уфа");
        o.setTelephone("89001234567");
        return o;
    }

    // ──────────────────────────────────────────────────────
    // Группа 1: Базовые CRUD-операции
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Базовые CRUD-операции")
    class CrudTests {

        @Test
        @DisplayName("1.1 Сохранение — ID генерируется автоматически")
        void saveOwnerGeneratesId() {
            Owner saved = ownerRepository.save(buildOwner("Тест", "Тестов"));
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isPositive();
        }

        @Test
        @DisplayName("1.2 Сохранение и поиск по ID — все поля совпадают")
        void saveAndFindById_allFieldsMatch() {
            Owner saved = ownerRepository.save(buildOwner("Иван", "Петров"));
            var found = ownerRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Иван");
            assertThat(found.get().getLastName()).isEqualTo("Петров");
            assertThat(found.get().getAddress()).isEqualTo("Тестовая ул., д. 1");
            assertThat(found.get().getCity()).isEqualTo("Уфа");
            assertThat(found.get().getTelephone()).isEqualTo("89001234567");
        }

        @Test
        @DisplayName("1.3 Поиск по несуществующему ID — пустой Optional")
        void findByMissingId_returnsEmpty() {
            assertThat(ownerRepository.findById(999999)).isEmpty();
        }

        @Test
        @DisplayName("1.4 Удаление — запись больше не находится")
        void deleteOwner_notFoundAfterDeletion() {
            Owner saved = ownerRepository.save(buildOwner("Удалить", "Меня"));
            Integer id = saved.getId();

            ownerRepository.deleteById(id);

            assertThat(ownerRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("1.5 Обновление — новые данные сохраняются в БД")
        void updateOwner_newDataPersisted() {
            Owner saved = ownerRepository.save(buildOwner("Старое", "Имя"));
            saved.setFirstName("Новое");
            saved.setCity("Казань");
            ownerRepository.save(saved);

            var found = ownerRepository.findById(saved.getId()).get();
            assertThat(found.getFirstName()).isEqualTo("Новое");
            assertThat(found.getCity()).isEqualTo("Казань");
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 2: Работа с несколькими записями
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Несколько записей")
    class MultipleRecordsTests {

        @Test
        @DisplayName("2.1 Два владельца получают разные ID")
        void twoOwners_getDifferentIds() {
            Owner o1 = ownerRepository.save(buildOwner("Первый", "Владелец"));
            Owner o2 = ownerRepository.save(buildOwner("Второй", "Владелец"));
            assertThat(o1.getId()).isNotEqualTo(o2.getId());
        }

        @Test
        @DisplayName("2.2 Удаление одного не затрагивает другого")
        void deleteOne_otherRemains() {
            Owner keep   = ownerRepository.save(buildOwner("Оставить", "Меня"));
            Owner delete = ownerRepository.save(buildOwner("Удалить",  "Меня"));

            ownerRepository.deleteById(delete.getId());

            assertThat(ownerRepository.findById(keep.getId())).isPresent();
            assertThat(ownerRepository.findById(delete.getId())).isEmpty();
        }

        @Test
        @DisplayName("2.3 Три владельца сохраняются независимо")
        void threeOwners_savedIndependently() {
            Owner o1 = ownerRepository.save(buildOwner("Первый",  "А"));
            Owner o2 = ownerRepository.save(buildOwner("Второй",  "Б"));
            Owner o3 = ownerRepository.save(buildOwner("Третий",  "В"));

            assertThat(ownerRepository.findById(o1.getId())).isPresent();
            assertThat(ownerRepository.findById(o2.getId())).isPresent();
            assertThat(ownerRepository.findById(o3.getId())).isPresent();
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 3: Граничные случаи и специфика PostgreSQL
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Граничные случаи")
    class EdgeCaseTests {

        @Test
        @DisplayName("3.1 Все поля владельца сохраняются без потерь")
        void allFieldsPersistCorrectly() {
            Owner owner = new Owner();
            owner.setFirstName("Александра");
            owner.setLastName("Тестировщикова");
            owner.setAddress("пр. Октября, д. 12, кв. 34");
            owner.setCity("Москва");
            owner.setTelephone("74951234567");
            Owner saved = ownerRepository.save(owner);

            var found = ownerRepository.findById(saved.getId()).get();
            assertThat(found.getAddress()).isEqualTo("пр. Октября, д. 12, кв. 34");
            assertThat(found.getCity()).isEqualTo("Москва");
            assertThat(found.getTelephone()).isEqualTo("74951234567");
        }

        @Test
        @DisplayName("3.2 Кириллические символы в именах сохраняются корректно")
        void cyrillicNames_savedCorrectly() {
            Owner owner = ownerRepository.save(buildOwner("Гузалия", "Гареева"));
            var found = ownerRepository.findById(owner.getId()).get();
            assertThat(found.getFirstName()).isEqualTo("Гузалия");
            assertThat(found.getLastName()).isEqualTo("Гареева");
        }

        @Test
        @DisplayName("3.3 ID сохранённого объекта совпадает с ID найденного")
        void savedId_matchesFoundId() {
            Owner saved = ownerRepository.save(buildOwner("Проверка", "ID"));
            var found = ownerRepository.findById(saved.getId()).get();
            assertThat(found.getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("3.4 Повторное сохранение не создаёт дубликат")
        void resave_doesNotCreateDuplicate() {
            Owner saved = ownerRepository.save(buildOwner("Дубликат", "Тест"));
            Integer originalId = saved.getId();
            saved.setCity("Новый город");
            Owner resaved = ownerRepository.save(saved);
            assertThat(resaved.getId()).isEqualTo(originalId);
        }

        @Test
        @DisplayName("3.5 Длинный адрес сохраняется без усечения")
        void longAddress_savedCompletely() {
            String longAddress = "Очень длинное название улицы с номером дома и квартиры 999";
            Owner owner = buildOwner("Тест", "Адреса");
            owner.setAddress(longAddress);
            Owner saved = ownerRepository.save(owner);
            assertThat(ownerRepository.findById(saved.getId()).get().getAddress())
                .isEqualTo(longAddress);
        }
    }
}
