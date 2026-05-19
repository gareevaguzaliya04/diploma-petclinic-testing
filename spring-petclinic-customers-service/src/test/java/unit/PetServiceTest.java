package org.springframework.samples.petclinic.customers.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import org.springframework.samples.petclinic.customers.model.Pet;
import org.springframework.samples.petclinic.customers.model.PetType;
import org.springframework.samples.petclinic.customers.model.Owner;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class PetServiceTest {

    @Nested
    @DisplayName("Модель Pet")
    class PetModelTests {

        @Test
        @DisplayName("Имя питомца сохраняется корректно")
        void shouldSetAndGetName() {
            Pet pet = new Pet();
            pet.setName("Buddy");
            assertThat(pet.getName()).isEqualTo("Buddy");
        }

        @Test
        @DisplayName("Новый питомец не имеет ID")
        void newPetShouldHaveNoId() {
            Pet pet = new Pet();
            assertThat(pet.getId()).isNull();
        }

        @Test
        @DisplayName("Дата рождения сохраняется корректно")
        void shouldSetBirthDate() {
            Pet pet = new Pet();
            Date birthDate = new Date();
            pet.setBirthDate(birthDate);
            assertThat(pet.getBirthDate()).isEqualTo(birthDate);
        }

        @Test
        @DisplayName("Тип питомца сохраняется корректно")
        void shouldSetPetType() {
            Pet pet = new Pet();
            PetType type = new PetType();
            type.setName("cat");
            pet.setType(type);
            assertThat(pet.getType().getName()).isEqualTo("cat");
        }
    }

    @Nested
    @DisplayName("Модель Owner")
    class OwnerModelTests {

        @Test
        @DisplayName("Имя владельца сохраняется корректно")
        void shouldSetFirstAndLastName() {
            Owner owner = new Owner();
            owner.setFirstName("John");
            owner.setLastName("Doe");
            assertThat(owner.getFirstName()).isEqualTo("John");
            assertThat(owner.getLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Новый владелец имеет пустой список питомцев")
        void newOwnerShouldHaveNoPets() {
            Owner owner = new Owner();
            assertThat(owner.getPets()).isEmpty();
        }

        @Test
        @DisplayName("Питомец добавляется к владельцу")
        void shouldAddPetToOwner() {
            Owner owner = new Owner();
            Pet pet = new Pet();
            pet.setName("Fluffy");
            owner.addPet(pet);
            assertThat(owner.getPets()).hasSize(1);
            assertThat(owner.getPets().get(0).getName()).isEqualTo("Fluffy");
        }
    }
}