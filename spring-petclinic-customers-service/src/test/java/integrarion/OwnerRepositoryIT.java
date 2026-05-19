package org.springframework.samples.petclinic.customers.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OwnerRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("customers_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OwnerRepository ownerRepository;

    @Test
    @DisplayName("Сохраняет владельца в реальную БД и находит его по ID")
    void shouldSaveAndFindOwnerById() {
        Owner owner = new Owner();
        owner.setFirstName("Иван");
        owner.setLastName("Петров");
        owner.setAddress("ул. Ленина 1");
        owner.setCity("Казань");
        owner.setTelephone("89001234567");

        Owner saved = ownerRepository.save(owner);

        assertThat(saved.getId()).isNotNull();

        Optional<Owner> found = ownerRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Иван");
        assertThat(found.get().getLastName()).isEqualTo("Петров");
    }

    @Test
    @DisplayName("Возвращает пустой Optional если владелец не найден")
    void shouldReturnEmpty_whenOwnerNotFound() {
        Optional<Owner> found = ownerRepository.findById(99999);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Удаляет владельца из БД")
    void shouldDeleteOwner() {
        Owner owner = new Owner();
        owner.setFirstName("Мария");
        owner.setLastName("Иванова");
        owner.setAddress("ул. Мира 5");
        owner.setCity("Москва");
        owner.setTelephone("89009876543");

        Owner saved = ownerRepository.save(owner);
        Integer id = saved.getId();

        ownerRepository.deleteById(id);

        assertThat(ownerRepository.findById(id)).isEmpty();
    }
}