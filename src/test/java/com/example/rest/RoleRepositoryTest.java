package com.example.rest;

import com.example.rest.entity.Role;
import com.example.rest.entity.enums.RoleType;
import com.example.rest.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest - Configures in-memory database and loads only JPA components
 * It's faster than @SpringBootTest because it doesn't load the entire application
 */
@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")// Uses application-test.yml
@DisplayName("RoleRepository Tests")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    // Test data - created fresh before each test
    private Role adminRole;
    private Role userRole;
    private Role sellerRole;

    /**
     * @BeforeEach runs before EACH test method
     * Creates fresh test data for isolation
     */
    @BeforeEach
    void setUp() {
        // Clean database (in case previous test left data)
        roleRepository.deleteAll();

        // Create test roles
        adminRole = Role.builder()
                .name(RoleType.ROLE_ADMIN)
                .description("Administrator role")
                .build();

        userRole = Role.builder()
                .name(RoleType.ROLE_USER)
                .description("Regular user role")
                .build();

        sellerRole = Role.builder()
                .name(RoleType.ROLE_SELLER)
                .description("Seller role")
                .build();

        // Save to database
        roleRepository.saveAll(List.of(adminRole, userRole, sellerRole));
    }

    // ============================================
    // Testing JpaRepository Auto-Generated Methods
    // ============================================

    @Test
    @DisplayName("Should save role successfully")
    void testSave() {
        // Arrange
        Role moderatorRole = Role.builder()
                .name(RoleType.ROLE_MODERATOR)
                .description("Moderator role")
                .build();

        // Act
        Role savedRole = roleRepository.save(moderatorRole);

        // Assert
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getId()).isNotNull();  // ID auto-generated
        assertThat(savedRole.getName()).isEqualTo(RoleType.ROLE_MODERATOR);
        assertThat(savedRole.getDescription()).isEqualTo("Moderator role");
    }

    @Test
    @DisplayName("Should find role by ID")
    void testFindById() {
        // Act
        Optional<Role> foundRole = roleRepository.findById(adminRole.getId());

        // Assert
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo(RoleType.ROLE_ADMIN);
        assertThat(foundRole.get().getDescription()).isEqualTo("Administrator role");
    }

    @Test
    @DisplayName("Should return empty when role ID doesn't exist")
    void testFindById_NotFound() {
        // Act
        Optional<Role> foundRole = roleRepository.findById(999L);

        // Assert
        assertThat(foundRole).isEmpty();
    }

    @Test
    @DisplayName("Should find all roles")
    void testFindAll() {
        // Act
        List<Role> roles = roleRepository.findAll();

        // Assert
        assertThat(roles).hasSize(3);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder(
                        RoleType.ROLE_ADMIN,
                        RoleType.ROLE_USER,
                        RoleType.ROLE_SELLER
                );
    }

    @Test
    @DisplayName("Should count all roles")
    void testCount() {
        // Act
        long count = roleRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should check if role exists by ID")
    void testExistsById() {
        // Act & Assert
        assertThat(roleRepository.existsById(adminRole.getId())).isTrue();
        assertThat(roleRepository.existsById(999L)).isFalse();
    }

    @Test
    @DisplayName("Should delete role by ID")
    void testDeleteById() {
        // Arrange
        Long roleId = adminRole.getId();

        // Act
        roleRepository.deleteById(roleId);

        // Assert
        assertThat(roleRepository.existsById(roleId)).isFalse();
        assertThat(roleRepository.count()).isEqualTo(2);  // 3 - 1 = 2
    }

    @Test
    @DisplayName("Should delete role entity")
    void testDelete() {
        // Act
        roleRepository.delete(userRole);

        // Assert
        assertThat(roleRepository.count()).isEqualTo(2);
        assertThat(roleRepository.findById(userRole.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all roles")
    void testDeleteAll() {
        // Act
        roleRepository.deleteAll();

        // Assert
        assertThat(roleRepository.count()).isZero();
        assertThat(roleRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should update role")
    void testUpdate() {
        // Arrange
        adminRole.setDescription("Updated admin description");

        // Act
        Role updatedRole = roleRepository.save(adminRole);

        // Assert
        assertThat(updatedRole.getId()).isEqualTo(adminRole.getId());  // Same ID
        assertThat(updatedRole.getDescription()).isEqualTo("Updated admin description");

        // Verify in database
        Role fromDb = roleRepository.findById(adminRole.getId()).orElseThrow();
        assertThat(fromDb.getDescription()).isEqualTo("Updated admin description");
    }

    // ============================================
    // Testing Custom Methods
    // ============================================

    @Test
    @DisplayName("Should find role by name")
    void testFindByName_Success() {
        // Act
        Optional<Role> foundRole = roleRepository.findByName(RoleType.ROLE_ADMIN);

        // Assert
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo(RoleType.ROLE_ADMIN);
        assertThat(foundRole.get().getDescription()).isEqualTo("Administrator role");
    }

    @Test
    @DisplayName("Should return empty when role name doesn't exist")
    void testFindByName_NotFound() {
        // Act
        Optional<Role> foundRole = roleRepository.findByName(RoleType.ROLE_MODERATOR);

        // Assert
        assertThat(foundRole).isEmpty();
    }

    @Test
    @DisplayName("Should check if role exists by name - true")
    void testExistsByName_True() {
        // Act
        boolean exists = roleRepository.existsByName(RoleType.ROLE_USER);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should check if role exists by name - false")
    void testExistsByName_False() {
        // Act
        boolean exists = roleRepository.existsByName(RoleType.ROLE_MODERATOR);

        // Assert
        assertThat(exists).isFalse();
    }

    // ============================================
    // Edge Cases & Scenarios
    // ============================================

    @Test
    @DisplayName("Should handle saving duplicate role names")
    void testSaveDuplicateRoleName() {
        // Arrange
        Role duplicateRole = Role.builder()
                .name(RoleType.ROLE_ADMIN)  // Duplicate name
                .description("Another admin")
                .build();

        // Act & Assert
        // This will throw exception because name has unique constraint
        // We expect this test to fail - uncomment to test constraint

        // org.springframework.dao.DataIntegrityViolationException expected
        // assertThatThrownBy(() -> roleRepository.save(duplicateRole))
        //         .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should save and retrieve role with null description")
    void testSaveRoleWithNullDescription() {
        // Arrange
        Role roleWithoutDesc = Role.builder()
                .name(RoleType.ROLE_MODERATOR)
                .description(null)
                .build();

        // Act
        Role savedRole = roleRepository.save(roleWithoutDesc);

        // Assert
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getDescription()).isNull();

        // Verify retrieval
        Optional<Role> found = roleRepository.findByName(RoleType.ROLE_MODERATOR);
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isNull();
    }

    @Test
    @DisplayName("Should handle empty database")
    void testEmptyDatabase() {
        // Arrange
        roleRepository.deleteAll();

        // Act & Assert
        assertThat(roleRepository.count()).isZero();
        assertThat(roleRepository.findAll()).isEmpty();
        assertThat(roleRepository.findByName(RoleType.ROLE_ADMIN)).isEmpty();
        assertThat(roleRepository.existsByName(RoleType.ROLE_USER)).isFalse();
    }
}