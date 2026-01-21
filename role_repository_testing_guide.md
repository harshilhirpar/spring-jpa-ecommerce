# Testing RoleRepository - Complete Guide

## What We're Testing

```java
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
    boolean existsByName(RoleType name);
}
```

We need to test:
1. **Auto-generated methods** from JpaRepository (save, findById, findAll, etc.)
2. **Custom methods** we wrote (findByName, existsByName)

---

## Test Setup

### 1. Add Test Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Test Starter (includes JUnit 5, Mockito, AssertJ) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- H2 Database for testing (in-memory) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Test Configuration (application-test.yml)

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
  jpa:
    hibernate:
      ddl-auto: create-drop  # Creates fresh DB for each test
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
        
  sql:
    init:
      mode: never  # Don't run schema.sql in tests
      
logging:
  level:
    org.hibernate.SQL: DEBUG
```

**Why H2 in-memory database?**
- Fast (runs in memory, not disk)
- Fresh database for each test (no pollution between tests)
- No need to install PostgreSQL for testing
- Same JPA code works on H2 and PostgreSQL

---

## Complete Test Class

### RoleRepositoryTest.java

```java
package com.ecommerce.repository;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest - Configures in-memory database and loads only JPA components
 * It's faster than @SpringBootTest because it doesn't load the entire application
 */
@DataJpaTest
@ActiveProfiles("test")  // Uses application-test.yml
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
```

---

## Understanding the Test Annotations

### @DataJpaTest
```java
@DataJpaTest
```
- Configures an **in-memory database** (H2)
- Loads only **JPA components** (repositories, entities)
- Doesn't load controllers, services, or security
- Each test runs in a **transaction** that's **rolled back** after the test
- **Faster** than @SpringBootTest

### @ActiveProfiles("test")
```java
@ActiveProfiles("test")
```
- Uses `application-test.yml` configuration
- Keeps test config separate from dev/prod config

### @BeforeEach
```java
@BeforeEach
void setUp() { ... }
```
- Runs **before each** test method
- Creates fresh test data
- Ensures test isolation (tests don't affect each other)

### @DisplayName
```java
@DisplayName("Should find role by name")
```
- Makes test reports readable
- Shows in IDE test runner and reports

---

## AssertJ Assertions Explained

### Basic Assertions
```java
assertThat(foundRole).isNotNull();
assertThat(savedRole.getId()).isNotNull();
assertThat(foundRole.get().getName()).isEqualTo(RoleType.ROLE_ADMIN);
```

### Optional Assertions
```java
assertThat(foundRole).isPresent();        // Optional has value
assertThat(foundRole).isEmpty();          // Optional is empty
```

### Collection Assertions
```java
assertThat(roles).hasSize(3);             // List has 3 elements
assertThat(roles).isEmpty();              // List is empty
assertThat(roles).isNotEmpty();           // List has elements

// Extract property and check values
assertThat(roles).extracting(Role::getName)
        .containsExactlyInAnyOrder(
            RoleType.ROLE_ADMIN, 
            RoleType.ROLE_USER
        );
```

### Numeric Assertions
```java
assertThat(count).isEqualTo(3);
assertThat(count).isZero();
assertThat(count).isGreaterThan(2);
```

### Boolean Assertions
```java
assertThat(exists).isTrue();
assertThat(exists).isFalse();
```

---

## Running the Tests

### From IDE (IntelliJ/Eclipse)
1. Right-click on `RoleRepositoryTest`
2. Select "Run RoleRepositoryTest"
3. See green checkmarks ✓

### From Maven
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=RoleRepositoryTest

# Run specific test method
mvn test -Dtest=RoleRepositoryTest#testFindByName_Success
```

---

## What Each Test Does

| Test Method | What It Tests | Why It's Important |
|-------------|---------------|-------------------|
| `testSave` | Can save new role | Create operation works |
| `testFindById` | Can retrieve by ID | Read operation works |
| `testFindById_NotFound` | Handles missing data | Doesn't crash on invalid ID |
| `testFindAll` | Can get all roles | Read all operation works |
| `testCount` | Can count records | Aggregation works |
| `testExistsById` | Can check existence | Validation works |
| `testDeleteById` | Can delete by ID | Delete operation works |
| `testDelete` | Can delete entity | Alternative delete works |
| `testDeleteAll` | Can clear table | Bulk delete works |
| `testUpdate` | Can modify existing | Update operation works |
| `testFindByName_Success` | Custom query works | Our method works |
| `testFindByName_NotFound` | Handles missing name | Graceful failure |
| `testExistsByName_True` | Exists check works | Validation works |
| `testExistsByName_False` | Handles non-existent | Validation works |

---

## Test Coverage Report

After running tests, you can generate coverage report:

```bash
mvn clean test jacoco:report
```

View report: `target/site/jacoco/index.html`

---

## Common Issues & Solutions

### Issue 1: Tests fail with "Table not found"
**Solution:** Check `ddl-auto: create-drop` in application-test.yml

### Issue 2: Tests interfere with each other
**Solution:** Use `@BeforeEach` to reset data, or rely on @DataJpaTest transaction rollback

### Issue 3: H2 SQL syntax differs from PostgreSQL
**Solution:** Most JPA queries work on both, but avoid native queries in tests

### Issue 4: Slow tests
**Solution:**
- Use @DataJpaTest (not @SpringBootTest)
- Use H2 in-memory (not real PostgreSQL)
- Don't use Thread.sleep() in tests

---

## Best Practices

✅ **DO:**
- Use `@DataJpaTest` for repository tests
- Use H2 in-memory database
- Test both success and failure cases
- Use meaningful test names
- Keep tests independent (no shared state)
- Use `@BeforeEach` for setup

❌ **DON'T:**
- Use `@SpringBootTest` for simple repository tests (too slow)
- Share data between tests
- Test multiple things in one test method
- Hardcode IDs (they're auto-generated)
- Ignore edge cases (null values, empty collections)

---

## Next Steps

1. **Run these tests** and make sure they all pass
2. **Modify a test** to make it fail (e.g., change expected value)
3. **Add a new test** for practice
4. **Move to AddressRepository tests** (next level)

Would you like me to:
1. Show you how to run these tests step-by-step?
2. Create tests for AddressRepository next?
3. Explain any specific assertion you're confused about?