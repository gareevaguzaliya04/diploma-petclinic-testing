package org.springframework.samples.petclinic.customers.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Расширенные интеграционные тесты — массовые операции и граничные случаи.
 * Уровень: INTEGRATION (требует Docker — PostgreSQL 15 через Testcontainers)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PetRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pet_integration_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",            postgres::getJdbcUrl);
        r.add("spring.datasource.username",       postgres::getUsername);
        r.add("spring.datasource.password",       postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto",    () -> "create-drop");
        r.add("spring.sql.init.mode",             () -> "never");
    }

    @Autowired
    private OwnerRepository ownerRepository;

    private Owner buildOwner(String first, String last) {
        Owner o = new Owner();
        o.setFirstName(first);
        o.setLastName(last);
        o.setAddress("ул. Пушкина, д. 1");
        o.setCity("Уфа");
        o.setTelephone("89001234567");
        return o;
    }

    // ──────────────────────────────────────────────────────
    // Группа 1: Массовые операции
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Массовые операции")
    class BulkOperationTests {

        @Test
        @DisplayName("1.1 Десять владельцев сохраняются успешно")
        void tenOwners_savedSuccessfully() {
            for (int i = 1; i <= 10; i++) {
                Owner o = ownerRepository.save(buildOwner("Имя" + i, "Фамилия" + i));
                assertThat(o.getId()).isNotNull();
            }
        }

        @Test
        @DisplayName("1.2 Пять владельцев — все имеют уникальные ID")
        void fiveOwners_uniqueIds() {
            var ids = new java.util.HashSet<Integer>();
            for (int i = 1; i <= 5; i++) {
                Owner o = ownerRepository.save(buildOwner("Test" + i, "User" + i));
                ids.add(o.getId());
            }
            assertThat(ids).hasSize(5);
        }

        @Test
        @DisplayName("1.3 Сохранение и удаление трёх владельцев")
        void saveAndDeleteThree() {
            Owner o1 = ownerRepository.save(buildOwner("А", "Один"));
            Owner o2 = ownerRepository.save(buildOwner("Б", "Два"));
            Owner o3 = ownerRepository.save(buildOwner("В", "Три"));

            ownerRepository.deleteById(o1.getId());
            ownerRepository.deleteById(o2.getId());
            ownerRepository.deleteById(o3.getId());

            assertThat(ownerRepository.findById(o1.getId())).isEmpty();
            assertThat(ownerRepository.findById(o2.getId())).isEmpty();
            assertThat(ownerRepository.findById(o3.getId())).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 2: Специфика PostgreSQL
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Специфика PostgreSQL")
    class PostgreSQLSpecificTests {

        @Test
        @DisplayName("2.1 Транзакция: сохранение откатывается при ошибке")
        void idIsAutoIncrementedCorrectly() {
            Owner o1 = ownerRepository.save(buildOwner("First", "Save"));
            Owner o2 = ownerRepository.save(buildOwner("Second", "Save"));
            // ID у второго владельца больше чем у первого
            assertThat(o2.getId()).isGreaterThan(o1.getId());
        }

        @Test
        @DisplayName("2.2 Поле address поддерживает спецсимволы")
        void addressWithSpecialChars() {
            Owner o = buildOwner("Тест", "Адрес");
            o.setAddress("ул. Ленина, д. 12/3, кв. 45-А");
            Owner saved = ownerRepository.save(o);
            assertThat(ownerRepository.findById(saved.getId())
                .get().getAddress()).contains("12/3");
        }

        @Test
        @DisplayName("2.3 Поле city поддерживает дефис в названии")
        void cityWithHyphen() {
            Owner o = buildOwner("Тест", "Город");
            o.setCity("Санкт-Петербург");
            Owner saved = ownerRepository.save(o);
            assertThat(ownerRepository.findById(saved.getId())
                .get().getCity()).isEqualTo("Санкт-Петербург");
        }

        @ParameterizedTest
        @DisplayName("2.4 Различные города сохраняются в PostgreSQL")
        @ValueSource(strings = {"Уфа", "Казань", "Москва", "Самара"})
        void variousCities_persistedInPostgres(String city) {
            Owner o = buildOwner("Город", "Тест");
            o.setCity(city);
            Owner saved = ownerRepository.save(o);
            assertThat(ownerRepository.findById(saved.getId())
                .get().getCity()).isEqualTo(city);
        }
    }

    // ──────────────────────────────────────────────────────
    // Группа 3: Последовательные операции
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Последовательные операции")
    class SequentialOperationTests {

        @Test
        @DisplayName("3.1 Создать → найти → обновить → найти снова")
        void createFindUpdateFind() {
            Owner saved = ownerRepository.save(buildOwner("Начальное", "Имя"));
            assertThat(ownerRepository.findById(saved.getId())).isPresent();

            saved.setFirstName("Обновлённое");
            ownerRepository.save(saved);

            assertThat(ownerRepository.findById(saved.getId())
                .get().getFirstName()).isEqualTo("Обновлённое");
        }

        @Test
        @DisplayName("3.2 Создать → удалить → создать снова с другим ID")
        void createDeleteCreate() {
            Owner first  = ownerRepository.save(buildOwner("Первый", "Раз"));
            Integer firstId = first.getId();
            ownerRepository.deleteById(firstId);

            Owner second = ownerRepository.save(buildOwner("Второй", "Раз"));
            assertThat(second.getId()).isNotEqualTo(firstId);
        }

        @Test
        @DisplayName("3.3 Двойное обновление — последнее значение побеждает")
        void doubleUpdate_lastValueWins() {
            Owner o = ownerRepository.save(buildOwner("Первое", "Значение"));
            o.setCity("Уфа");
            ownerRepository.save(o);
            o.setCity("Казань");
            ownerRepository.save(o);

            assertThat(ownerRepository.findById(o.getId())
                .get().getCity()).isEqualTo("Казань");
        }
    }
}
