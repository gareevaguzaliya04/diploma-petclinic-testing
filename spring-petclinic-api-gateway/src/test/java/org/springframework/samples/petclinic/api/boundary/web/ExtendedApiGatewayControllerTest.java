package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.VisitDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebFluxTest(controllers = ApiGatewayController.class)
@Import({ReactiveResilience4JAutoConfiguration.class, CircuitBreakerConfiguration.class})
class ExtendedApiGatewayControllerTest {

    @MockitoBean
    private CustomersServiceClient customersServiceClient;

    @MockitoBean
    private VisitsServiceClient visitsServiceClient;

    @Autowired
    private WebTestClient client;

    // ──────────────────────────────────────────────────────
    // 1. Владелец без питомцев
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Владелец без питомцев")
    class OwnerWithNoPets {

        @Test
        @DisplayName("1.1 Владелец без питомцев → 200, pets пустой список")
        void noPets_returnsEmptyPetsList() {
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .id(1)
                .firstName("Иван")
                .lastName("Петров")
                .pets(List.of())
                .build();
            Mockito.when(customersServiceClient.getOwner(1)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(Collections.emptyList()))
                .thenReturn(Mono.just(new Visits(List.of())));

            client.get()
                .uri("/api/gateway/owners/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets").isArray()
                .jsonPath("$.pets.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("1.2 Владелец без питомцев — firstName и lastName в ответе")
        void noPets_ownerFieldsPresent() {
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .id(2)
                .firstName("Гузалия")
                .lastName("Гареева")
                .address("ул. Ленина")
                .city("Уфа")
                .telephone("89001234567")
                .pets(List.of())
                .build();
            Mockito.when(customersServiceClient.getOwner(2)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(Collections.emptyList()))
                .thenReturn(Mono.just(new Visits(List.of())));

            client.get()
                .uri("/api/gateway/owners/2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Гузалия")
                .jsonPath("$.lastName").isEqualTo("Гареева");
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. Правильное распределение визитов по питомцам
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Распределение визитов по питомцам")
    class VisitDistribution {

        @Test
        @DisplayName("2.1 Два питомца — визиты попадают к правильному питомцу")
        void twoPets_visitsAssignedCorrectly() {
            PetDetails cat = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(10).name("Мурзик").visits(new ArrayList<>()).build();
            PetDetails dog = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(20).name("Рекс").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(cat, dog)).build();

            Mockito.when(customersServiceClient.getOwner(1)).thenReturn(Mono.just(owner));

            VisitDetails catVisit = new VisitDetails(1, 10, "2024-01-01", "Осмотр кота");
            VisitDetails dogVisit = new VisitDetails(2, 20, "2024-01-02", "Прививка пса");
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(10, 20)))
                .thenReturn(Mono.just(new Visits(List.of(catVisit, dogVisit))));

            client.get()
                .uri("/api/gateway/owners/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].visits[0].description").isEqualTo("Осмотр кота")
                .jsonPath("$.pets[1].visits[0].description").isEqualTo("Прививка пса");
        }

        @Test
        @DisplayName("2.2 Один питомец с несколькими визитами — все присвоены ему")
        void onePetMultipleVisits_allAssigned() {
            PetDetails cat = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(5).name("Барсик").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(cat)).build();

            Mockito.when(customersServiceClient.getOwner(3)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(5)))
                .thenReturn(Mono.just(new Visits(List.of(
                    new VisitDetails(1, 5, "2024-01-01", "Первый"),
                    new VisitDetails(2, 5, "2024-02-01", "Второй"),
                    new VisitDetails(3, 5, "2024-03-01", "Третий")
                ))));

            client.get()
                .uri("/api/gateway/owners/3")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].visits.length()").isEqualTo(3);
        }

        @Test
        @DisplayName("2.3 Питомцу без визитов — visits пустой список")
        void petWithNoVisits_emptyVisitsList() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(7).name("Пушок").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(pet)).build();

            Mockito.when(customersServiceClient.getOwner(4)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(7)))
                .thenReturn(Mono.just(new Visits(List.of())));

            client.get()
                .uri("/api/gateway/owners/4")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].visits").isArray()
                .jsonPath("$.pets[0].visits.length()").isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. Circuit Breaker и отказы сервисов
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Circuit Breaker — отказы сервисов")
    class CircuitBreakerScenarios {

        @Test
        @DisplayName("3.1 visits-service недоступен → CB срабатывает, visits пустые")
        void visitsServiceDown_circuitBreaker_emptyVisits() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(10).name("Гарфилд").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(pet)).build();

            Mockito.when(customersServiceClient.getOwner(1)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(10)))
                .thenReturn(Mono.error(new ConnectException("visits-service down")));

            client.get()
                .uri("/api/gateway/owners/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].name").isEqualTo("Гарфилд")
                .jsonPath("$.pets[0].visits").isArray()
                .jsonPath("$.pets[0].visits.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("3.2 visits-service возвращает RuntimeException → CB возвращает пустые визиты")
        void visitsServiceRuntimeError_circuitBreaker_emptyVisits() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(15).name("Рекс").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(pet)).build();

            Mockito.when(customersServiceClient.getOwner(5)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(15)))
                .thenReturn(Mono.error(new RuntimeException("Internal error")));

            client.get()
                .uri("/api/gateway/owners/5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].visits").isEmpty();
        }

        @Test
        @DisplayName("3.3 Оба сервиса работают → 200, данные корректны")
        void bothServicesAvailable_200WithData() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(30).name("Барсик").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .id(10)
                .firstName("Тест")
                .pets(List.of(pet))
                .build();

            Mockito.when(customersServiceClient.getOwner(10)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(30)))
                .thenReturn(Mono.just(new Visits(List.of(
                    new VisitDetails(100, 30, "2024-01-15", "Плановый осмотр")
                ))));

            client.get()
                .uri("/api/gateway/owners/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.pets[0].name").isEqualTo("Барсик")
                .jsonPath("$.pets[0].visits[0].description").isEqualTo("Плановый осмотр");
        }
    }

    // ──────────────────────────────────────────────────────
    // 4. Данные в ответе
    // ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. Структура и данные ответа")
    class ResponseData {

        @Test
        @DisplayName("4.1 Имя питомца сохраняется в ответе")
        void petName_inResponse() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(1).name("Мурзик").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(pet)).build();

            Mockito.when(customersServiceClient.getOwner(1)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(1)))
                .thenReturn(Mono.just(new Visits(List.of())));

            client.get()
                .uri("/api/gateway/owners/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].name").isEqualTo("Мурзик");
        }

        @Test
        @DisplayName("4.2 Описание визита сохраняется без изменений")
        void visitDescription_unchangedInResponse() {
            PetDetails pet = PetDetails.PetDetailsBuilder.aPetDetails()
                .id(1).name("Кот").visits(new ArrayList<>()).build();
            OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .pets(List.of(pet)).build();

            Mockito.when(customersServiceClient.getOwner(1)).thenReturn(Mono.just(owner));
            Mockito.when(visitsServiceClient.getVisitsForPets(List.of(1)))
                .thenReturn(Mono.just(new Visits(List.of(
                    new VisitDetails(1, 1, null, "Ежегодная вакцинация")
                ))));

            client.get()
                .uri("/api/gateway/owners/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pets[0].visits[0].description").isEqualTo("Ежегодная вакцинация");
        }
    }
}
