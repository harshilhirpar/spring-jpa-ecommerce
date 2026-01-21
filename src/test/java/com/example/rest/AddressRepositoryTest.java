package com.example.rest;

import com.example.rest.entity.Address;
import com.example.rest.entity.User;
import com.example.rest.entity.enums.AddressType;
import com.example.rest.entity.enums.UserStatusEnum;
import com.example.rest.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
@DisplayName("AddressRepository Tests")
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    /**
     * TestEntityManager - Special tool for testing JPA entities
     * - persist() - saves entity to test database
     * - flush() - forces immediate database write
     * - clear() - clears persistence context
     * Use it when you need fine control over entity state
     */
    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private User user1;
    private User user2;

    private Address user1HomeAddress;
    private Address user1WorkAddress;
    private Address user1ShippingAddress;
    private Address user2HomeAddress;

    @BeforeEach
    void setUp() {
        // Clean all data
        addressRepository.deleteAll();

        // Create test users
        user1 = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatusEnum.ACTIVE)
                .build();

        user2 = User.builder()
                .username("jane_smith")
                .email("jane@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .status(UserStatusEnum.ACTIVE)
                .build();

        // Persist users first (addresses need user IDs)
        user1 = entityManager.persist(user1);
        user2 = entityManager.persist(user2);
        entityManager.flush();  // Ensure users get IDs

        // Create addresses for user1
        user1HomeAddress = Address.builder()
                .street("123 Main St")
                .city("Toronto")
                .state("Ontario")
                .postalCode("M5G 1E2")
                .country("Canada")
                .type(AddressType.HOME)
                .isDefault(true)
                .user(user1)
                .build();

        user1WorkAddress = Address.builder()
                .street("456 Bay St")
                .city("Toronto")
                .state("Ontario")
                .postalCode("M5H 2Y4")
                .country("Canada")
                .type(AddressType.WORK)
                .isDefault(false)
                .user(user1)
                .build();

        user1ShippingAddress = Address.builder()
                .street("789 King St")
                .city("Ottawa")
                .state("Ontario")
                .postalCode("K1A 0B1")
                .country("Canada")
                .type(AddressType.SHIPPING)
                .isDefault(false)
                .user(user1)
                .build();

        // Create address for user2
        user2HomeAddress = Address.builder()
                .street("321 Queen St")
                .city("Vancouver")
                .state("British Columbia")
                .postalCode("V6B 2W2")
                .country("Canada")
                .type(AddressType.HOME)
                .isDefault(true)
                .user(user2)
                .build();

        // Save all addresses
        addressRepository.saveAll(List.of(
                user1HomeAddress,
                user1WorkAddress,
                user1ShippingAddress,
                user2HomeAddress
        ));
    }

    // ============================================
    // Testing JpaRepository Auto-Generated Methods
    // ============================================

    @Test
    @DisplayName("Should save address successfully")
    void testSave() {
        // Arrange
        Address billingAddress = Address.builder()
                .street("999 Billing Ave")
                .city("Toronto")
                .state("Ontario")
                .postalCode("M4B 1B3")
                .country("Canada")
                .type(AddressType.BILLING)
                .isDefault(false)
                .user(user1)
                .build();

        // Act
        Address savedAddress = addressRepository.save(billingAddress);

        // Assert
        assertThat(savedAddress).isNotNull();
        assertThat(savedAddress.getId()).isNotNull();
        assertThat(savedAddress.getStreet()).isEqualTo("999 Billing Ave");
        assertThat(savedAddress.getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("Should find address by ID with user relationship loaded")
    void testFindById() {
        // Act
        Optional<Address> foundAddress = addressRepository.findById(user1HomeAddress.getId());

        // Assert
        assertThat(foundAddress).isPresent();
        assertThat(foundAddress.get().getStreet()).isEqualTo("123 Main St");
        assertThat(foundAddress.get().getCity()).isEqualTo("Toronto");

        // Verify relationship
        assertThat(foundAddress.get().getUser()).isNotNull();
        assertThat(foundAddress.get().getUser().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should find all addresses")
    void testFindAll() {
        // Act
        List<Address> addresses = addressRepository.findAll();

        // Assert
        assertThat(addresses).hasSize(4);  // 3 for user1 + 1 for user2
    }

    @Test
    @DisplayName("Should count all addresses")
    void testCount() {
        // Act
        long count = addressRepository.count();

        // Assert
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("Should delete address by ID")
    void testDeleteById() {
        // Arrange
        Long addressId = user1WorkAddress.getId();

        // Act
        addressRepository.deleteById(addressId);

        // Assert
        assertThat(addressRepository.existsById(addressId)).isFalse();
        assertThat(addressRepository.count()).isEqualTo(3);  // 4 - 1 = 3
    }

    @Test
    @DisplayName("Should update address")
    void testUpdate() {
        // Arrange
        user1HomeAddress.setStreet("999 Updated St");
        user1HomeAddress.setPostalCode("M1M 1M1");

        // Act
        Address updatedAddress = addressRepository.save(user1HomeAddress);

        // Assert
        assertThat(updatedAddress.getId()).isEqualTo(user1HomeAddress.getId());
        assertThat(updatedAddress.getStreet()).isEqualTo("999 Updated St");
        assertThat(updatedAddress.getPostalCode()).isEqualTo("M1M 1M1");

        // Verify in database
        Address fromDb = addressRepository.findById(user1HomeAddress.getId()).orElseThrow();
        assertThat(fromDb.getStreet()).isEqualTo("999 Updated St");
    }

    // ============================================
    // Testing Relationship Queries
    // ============================================

    @Test
    @DisplayName("Should find all addresses for a user")
    void testFindByUserId() {
        // Act
        List<Address> user1Addresses = addressRepository.findByUserId(user1.getId());

        // Assert
        assertThat(user1Addresses).hasSize(3);
        assertThat(user1Addresses)
                .extracting(Address::getStreet)
                .containsExactlyInAnyOrder(
                        "123 Main St",
                        "456 Bay St",
                        "789 King St"
                );

        // Verify all addresses belong to user1
        assertThat(user1Addresses)
                .allMatch(addr -> addr.getUser().getId().equals(user1.getId()));
    }

    @Test
    @DisplayName("Should find addresses by user ID and type")
    void testFindByUserIdAndType() {
        // Act
        List<Address> homeAddresses = addressRepository.findByUserIdAndType(
                user1.getId(),
                AddressType.HOME
        );

        // Assert
        assertThat(homeAddresses).hasSize(1);
        assertThat(homeAddresses.get(0).getStreet()).isEqualTo("123 Main St");
        assertThat(homeAddresses.get(0).getType()).isEqualTo(AddressType.HOME);
    }

    @Test
    @DisplayName("Should find multiple addresses of same type for user")
    void testFindByUserIdAndType_MultipleResults() {
        // Arrange - Add another shipping address for user1
        Address anotherShipping = Address.builder()
                .street("111 Second Shipping St")
                .city("Toronto")
                .state("Ontario")
                .postalCode("M3M 3M3")
                .country("Canada")
                .type(AddressType.SHIPPING)
                .isDefault(false)
                .user(user1)
                .build();
        addressRepository.save(anotherShipping);

        // Act
        List<Address> shippingAddresses = addressRepository.findByUserIdAndType(
                user1.getId(),
                AddressType.SHIPPING
        );

        // Assert
        assertThat(shippingAddresses).hasSize(2);
        assertThat(shippingAddresses)
                .allMatch(addr -> addr.getType() == AddressType.SHIPPING);
    }

    @Test
    @DisplayName("Should return empty list when user has no addresses of type")
    void testFindByUserIdAndType_NoResults() {
        // Act - user2 has no WORK addresses
        List<Address> workAddresses = addressRepository.findByUserIdAndType(
                user2.getId(),
                AddressType.WORK
        );

        // Assert
        assertThat(workAddresses).isEmpty();
    }

    @Test
    @DisplayName("Should find default address for user")
    void testFindByUserIdAndIsDefaultTrue() {
        // Act
        Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(user1.getId());

        // Assert
        assertThat(defaultAddress).isPresent();
        assertThat(defaultAddress.get().getStreet()).isEqualTo("123 Main St");
        assertThat(defaultAddress.get().getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("Should return empty when user has no default address")
    void testFindByUserIdAndIsDefaultTrue_NoDefault() {
        // Arrange - Set all user1 addresses to non-default
        user1HomeAddress.setIsDefault(false);
        user1WorkAddress.setIsDefault(false);
        user1ShippingAddress.setIsDefault(false);
        addressRepository.saveAll(List.of(user1HomeAddress, user1WorkAddress, user1ShippingAddress));

        // Act
        Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(user1.getId());

        // Assert
        assertThat(defaultAddress).isEmpty();
    }

    @Test
    @DisplayName("Should count addresses for a user")
    void testCountByUserId() {
        // Act
        long user1Count = addressRepository.countByUserId(user1.getId());
        long user2Count = addressRepository.countByUserId(user2.getId());

        // Assert
        assertThat(user1Count).isEqualTo(3);
        assertThat(user2Count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should delete all addresses for a user")
    void testDeleteByUserId() {
        // Arrange
        long initialCount = addressRepository.count();

        // Act
        long deletedCount = addressRepository.deleteByUserId(user1.getId());

        // Assert
        assertThat(deletedCount).isEqualTo(3);  // 3 addresses deleted
        assertThat(addressRepository.count()).isEqualTo(initialCount - 3);
        assertThat(addressRepository.findByUserId(user1.getId())).isEmpty();

        // Verify user2 addresses still exist
        assertThat(addressRepository.findByUserId(user2.getId())).hasSize(1);
    }

    // ============================================
    // Testing Non-Relationship Queries
    // ============================================

    @Test
    @DisplayName("Should find addresses by city")
    void testFindByCity() {
        // Act
        List<Address> torontoAddresses = addressRepository.findByCity("Toronto");

        // Assert
        assertThat(torontoAddresses).hasSize(2);  // user1 home and work
        assertThat(torontoAddresses)
                .extracting(Address::getCity)
                .containsOnly("Toronto");
    }

    @Test
    @DisplayName("Should find addresses by city - case sensitive")
    void testFindByCity_CaseSensitive() {
        // Act
        List<Address> addresses = addressRepository.findByCity("toronto");  // lowercase

        // Assert
        assertThat(addresses).isEmpty();  // findByCity is case-sensitive
    }

    // ============================================
    // Testing Custom JPQL Query
    // ============================================

    @Test
    @DisplayName("Should find addresses by postal code prefix")
    void testFindByPostalCodePrefix() {
        // Act
        List<Address> m5Addresses = addressRepository.findByPostalCodePrefix("M5");

        // Assert
        assertThat(m5Addresses).hasSize(2);  // M5G and M5H
        assertThat(m5Addresses)
                .extracting(Address::getPostalCode)
                .containsExactlyInAnyOrder("M5G 1E2", "M5H 2Y4");
    }

    @Test
    @DisplayName("Should find addresses with single character prefix")
    void testFindByPostalCodePrefix_SingleChar() {
        // Act
        List<Address> mAddresses = addressRepository.findByPostalCodePrefix("M");

        // Assert
        assertThat(mAddresses).hasSize(2);  // All Toronto/Ottawa addresses start with M
    }

    @Test
    @DisplayName("Should return empty list for non-matching prefix")
    void testFindByPostalCodePrefix_NoMatch() {
        // Act
        List<Address> addresses = addressRepository.findByPostalCodePrefix("X");

        // Assert
        assertThat(addresses).isEmpty();
    }

    // ============================================
    // Edge Cases & Real-World Scenarios
    // ============================================

    @Test
    @DisplayName("Should handle user with no addresses")
    void testUserWithNoAddresses() {
        // Arrange
        User user3 = User.builder()
                .username("new_user")
                .email("new@example.com")
                .password("password123")
                .status(UserStatusEnum.ACTIVE)
                .build();
        user3 = entityManager.persist(user3);
        entityManager.flush();

        // Act & Assert
        assertThat(addressRepository.findByUserId(user3.getId())).isEmpty();
        assertThat(addressRepository.countByUserId(user3.getId())).isZero();
        assertThat(addressRepository.findByUserIdAndIsDefaultTrue(user3.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should save address with minimal required fields")
    void testSaveMinimalAddress() {
        // Arrange - Only required fields
        Address minimalAddress = Address.builder()
                .street("999 Min St")
                .city("Toronto")
                .state("ON")
                .postalCode("M1M1M1")
                .country("Canada")
                .user(user1)
                .build();
        // Note: type and isDefault are null

        // Act
        Address saved = addressRepository.save(minimalAddress);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isNull();
        assertThat(saved.getIsDefault()).isNull();
    }

    @Test
    @DisplayName("Scenario: User checkout - get default shipping address")
    void testCheckoutScenario() {
        // Scenario: User is checking out, we need their default address for shipping

        // Act
        Optional<Address> shippingAddress = addressRepository.findByUserIdAndIsDefaultTrue(user1.getId());

        // Assert
        assertThat(shippingAddress).isPresent();
        assertThat(shippingAddress.get().getStreet()).isEqualTo("123 Main St");

        // In real app, this would be used to pre-fill shipping form
    }

    @Test
    @DisplayName("Scenario: Address book - show all user addresses grouped by type")
    void testAddressBookScenario() {
        // Scenario: Display user's address book

        // Act
        List<Address> allAddresses = addressRepository.findByUserId(user1.getId());

        // Group by type (in real app, you'd do this in service layer)
        long homeCount = allAddresses.stream()
                .filter(a -> a.getType() == AddressType.HOME)
                .count();
        long workCount = allAddresses.stream()
                .filter(a -> a.getType() == AddressType.WORK)
                .count();

        // Assert
        assertThat(allAddresses).hasSize(3);
        assertThat(homeCount).isEqualTo(1);
        assertThat(workCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario: Enforce address limit per user")
    void testAddressLimitScenario() {
        // Scenario: Business rule - max 5 addresses per user
        final int MAX_ADDRESSES = 5;

        // Act
        long currentCount = addressRepository.countByUserId(user1.getId());

        // Assert
        if (currentCount >= MAX_ADDRESSES) {
            // Would reject adding new address
            assertThat(currentCount).isGreaterThanOrEqualTo(MAX_ADDRESSES);
        } else {
            // Can add more addresses
            assertThat(currentCount).isLessThan(MAX_ADDRESSES);
        }

        // In this test, user1 has 3 addresses, so can add 2 more
        assertThat(currentCount).isEqualTo(3);
    }
}
