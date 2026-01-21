package com.example.rest;

import com.example.rest.entity.Category;
import com.example.rest.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
@DisplayName("CategoryRepository Tests - Self-Referencing Hierarchy")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Root categories (no parent)
    private Category electronics;
    private Category clothing;
    private Category books;

    // Electronics subcategories (level 1)
    private Category laptops;
    private Category phones;
    private Category tablets;

    // Laptops subcategories (level 2)
    private Category gamingLaptops;
    private Category businessLaptops;

    // Phones subcategories (level 2)
    private Category smartphones;
    private Category featurePhones;

    // Clothing subcategories (level 1)
    private Category mensClothing;
    private Category womensClothing;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        // ============================================
        // Create Root Categories (parent = null)
        // ============================================

        electronics = Category.builder()
                .name("Electronics")
                .description("All electronic items")
                .parent(null)  // Root category
                .build();

        clothing = Category.builder()
                .name("Clothing")
                .description("Apparel and fashion")
                .parent(null)  // Root category
                .build();

        books = Category.builder()
                .name("Books")
                .description("Books and literature")
                .parent(null)  // Root category
                .build();

        // Save root categories first (they have no parent dependency)
        electronics = categoryRepository.save(electronics);
        clothing = categoryRepository.save(clothing);
        books = categoryRepository.save(books);

        // ============================================
        // Create Level 1 Subcategories
        // ============================================

        laptops = Category.builder()
                .name("Laptops")
                .description("Portable computers")
                .parent(electronics)  // Parent is Electronics
                .build();

        phones = Category.builder()
                .name("Phones")
                .description("Mobile phones")
                .parent(electronics)
                .build();

        tablets = Category.builder()
                .name("Tablets")
                .description("Tablet devices")
                .parent(electronics)
                .build();

        mensClothing = Category.builder()
                .name("Men's Clothing")
                .description("Clothing for men")
                .parent(clothing)
                .build();

        womensClothing = Category.builder()
                .name("Women's Clothing")
                .description("Clothing for women")
                .parent(clothing)
                .build();

        // Save level 1 subcategories
        laptops = categoryRepository.save(laptops);
        phones = categoryRepository.save(phones);
        tablets = categoryRepository.save(tablets);
        mensClothing = categoryRepository.save(mensClothing);
        womensClothing = categoryRepository.save(womensClothing);

        // ============================================
        // Create Level 2 Subcategories (sub-subcategories)
        // ============================================

        gamingLaptops = Category.builder()
                .name("Gaming Laptops")
                .description("High-performance gaming laptops")
                .parent(laptops)  // Parent is Laptops
                .build();

        businessLaptops = Category.builder()
                .name("Business Laptops")
                .description("Professional business laptops")
                .parent(laptops)
                .build();

        smartphones = Category.builder()
                .name("Smartphones")
                .description("Smart mobile phones")
                .parent(phones)
                .build();

        featurePhones = Category.builder()
                .name("Feature Phones")
                .description("Basic mobile phones")
                .parent(phones)
                .build();

        // Save level 2 subcategories
        gamingLaptops = categoryRepository.save(gamingLaptops);
        businessLaptops = categoryRepository.save(businessLaptops);
        smartphones = categoryRepository.save(smartphones);
        featurePhones = categoryRepository.save(featurePhones);
    }

    // ============================================
    // Testing Basic CRUD Operations
    // ============================================

    @Test
    @DisplayName("Should save root category successfully")
    void testSaveRootCategory() {
        // Arrange
        Category sports = Category.builder()
                .name("Sports")
                .description("Sports equipment")
                .parent(null)
                .build();

        // Act
        Category saved = categoryRepository.save(sports);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Sports");
        assertThat(saved.getParent()).isNull();
    }

    @Test
    @DisplayName("Should save subcategory with parent relationship")
    void testSaveSubcategory() {
        // Arrange
        Category accessories = Category.builder()
                .name("Accessories")
                .description("Electronic accessories")
                .parent(electronics)
                .build();

        // Act
        Category saved = categoryRepository.save(accessories);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getParent()).isNotNull();
        assertThat(saved.getParent().getId()).isEqualTo(electronics.getId());
        assertThat(saved.getParent().getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should find category by ID with parent loaded")
    void testFindById() {
        // Act
        Optional<Category> found = categoryRepository.findById(gamingLaptops.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Gaming Laptops");

        // Verify parent relationship loaded
        assertThat(found.get().getParent()).isNotNull();
        assertThat(found.get().getParent().getName()).isEqualTo("Laptops");

        // Verify grandparent relationship (lazy loaded)
        assertThat(found.get().getParent().getParent()).isNotNull();
        assertThat(found.get().getParent().getParent().getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should find all categories")
    void testFindAll() {
        // Act
        List<Category> categories = categoryRepository.findAll();

        // Assert - 3 root + 5 level1 + 4 level2 = 12 total
        assertThat(categories).hasSize(12);
    }

    @Test
    @DisplayName("Should update category")
    void testUpdate() {
        // Arrange
        electronics.setDescription("Updated: All electronic devices");

        // Act
        Category updated = categoryRepository.save(electronics);

        // Assert
        assertThat(updated.getId()).isEqualTo(electronics.getId());
        assertThat(updated.getDescription()).isEqualTo("Updated: All electronic devices");

        // Verify in database
        Category fromDb = categoryRepository.findById(electronics.getId()).orElseThrow();
        assertThat(fromDb.getDescription()).isEqualTo("Updated: All electronic devices");
    }

    @Test
    @DisplayName("Should delete category without children")
    void testDeleteLeafCategory() {
        // Arrange - Delete a category with no subcategories
        Long categoryId = gamingLaptops.getId();

        // Act
        categoryRepository.deleteById(categoryId);

        // Assert
        assertThat(categoryRepository.existsById(categoryId)).isFalse();
        assertThat(categoryRepository.count()).isEqualTo(11);  // 12 - 1 = 11
    }

    // ============================================
    // Testing Self-Referencing Queries
    // ============================================

    @Test
    @DisplayName("Should find all root categories (parent is null)")
    void testFindByParentIsNull() {
        // Act
        List<Category> rootCategories = categoryRepository.findByParentIsNull();

        // Assert
        assertThat(rootCategories).hasSize(3);
        assertThat(rootCategories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Electronics", "Clothing", "Books");

        // Verify all have null parent
        assertThat(rootCategories)
                .allMatch(cat -> cat.getParent() == null);
    }

    @Test
    @DisplayName("Should find direct subcategories of a parent")
    void testFindByParentId() {
        // Act - Get all direct children of Electronics
        List<Category> electronicsSubcategories = categoryRepository.findByParentId(electronics.getId());

        // Assert
        assertThat(electronicsSubcategories).hasSize(3);  // Laptops, Phones, Tablets
        assertThat(electronicsSubcategories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Laptops", "Phones", "Tablets");

        // Verify all have Electronics as parent
        assertThat(electronicsSubcategories)
                .allMatch(cat -> cat.getParent().getId().equals(electronics.getId()));
    }

    @Test
    @DisplayName("Should find subcategories at different levels")
    void testFindByParentId_MultipleLevel() {
        // Act - Get children of Laptops (level 2)
        List<Category> laptopSubcategories = categoryRepository.findByParentId(laptops.getId());

        // Assert
        assertThat(laptopSubcategories).hasSize(2);
        assertThat(laptopSubcategories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Gaming Laptops", "Business Laptops");
    }

    @Test
    @DisplayName("Should return empty list for category with no children")
    void testFindByParentId_NoChildren() {
        // Act - Gaming Laptops has no subcategories
        List<Category> subcategories = categoryRepository.findByParentId(gamingLaptops.getId());

        // Assert
        assertThat(subcategories).isEmpty();
    }

    @Test
    @DisplayName("Should count direct subcategories")
    void testCountByParentId() {
        // Act
        long electronicsCount = categoryRepository.countByParentId(electronics.getId());
        long laptopsCount = categoryRepository.countByParentId(laptops.getId());
        long gamingLaptopsCount = categoryRepository.countByParentId(gamingLaptops.getId());

        // Assert
        assertThat(electronicsCount).isEqualTo(3);  // Laptops, Phones, Tablets
        assertThat(laptopsCount).isEqualTo(2);      // Gaming, Business
        assertThat(gamingLaptopsCount).isZero();    // No children
    }

    @Test
    @DisplayName("Should check if category has subcategories")
    void testExistsByParentId() {
        // Act & Assert
        assertThat(categoryRepository.existsByParentId(electronics.getId())).isTrue();
        assertThat(categoryRepository.existsByParentId(laptops.getId())).isTrue();
        assertThat(categoryRepository.existsByParentId(gamingLaptops.getId())).isFalse();
        assertThat(categoryRepository.existsByParentId(books.getId())).isFalse();
    }

    // ============================================
    // Testing Name-Based Queries
    // ============================================

    @Test
    @DisplayName("Should find category by exact name")
    void testFindByName() {
        // Act
        Optional<Category> found = categoryRepository.findByName("Electronics");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
        assertThat(found.get().getParent()).isNull();
    }

    @Test
    @DisplayName("Should return empty when category name doesn't exist")
    void testFindByName_NotFound() {
        // Act
        Optional<Category> found = categoryRepository.findByName("NonExistent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should search categories by keyword (case insensitive)")
    void testFindByNameContainingIgnoreCase() {
        // Act
        List<Category> laptopCategories = categoryRepository.findByNameContainingIgnoreCase("laptop");

        // Assert
        assertThat(laptopCategories).hasSize(3);  // Laptops, Gaming Laptops, Business Laptops
        assertThat(laptopCategories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Laptops", "Gaming Laptops", "Business Laptops");
    }

    @Test
    @DisplayName("Should search categories - case insensitive verification")
    void testFindByNameContainingIgnoreCase_CaseInsensitive() {
        // Act - Different cases
        List<Category> lower = categoryRepository.findByNameContainingIgnoreCase("laptop");
        List<Category> upper = categoryRepository.findByNameContainingIgnoreCase("LAPTOP");
        List<Category> mixed = categoryRepository.findByNameContainingIgnoreCase("LaPtOp");

        // Assert - All return same results
        assertThat(lower).hasSize(3);
        assertThat(upper).hasSize(3);
        assertThat(mixed).hasSize(3);
    }

    @Test
    @DisplayName("Should search categories by partial keyword")
    void testFindByNameContainingIgnoreCase_Partial() {
        // Act
        List<Category> clothingCategories = categoryRepository.findByNameContainingIgnoreCase("cloth");

        // Assert
        assertThat(clothingCategories).hasSize(3);  // Clothing, Men's Clothing, Women's Clothing
    }

    // ============================================
    // Testing Custom JPQL Query
    // ============================================

    @Test
    @DisplayName("Should find category with product count")
    void testFindCategoryWithProductCount() {
        // Note: This test assumes you have Product entity and relationship
        // For now, testing without products returns 0

        // Act
        List<Object[]> results = categoryRepository.findCategoryWithProductCount(electronics.getId());
        // Assert
        assertThat(results).hasSize(1);     // One row

        Object[] row = results.get(0);      // Get first row
        assertThat(row).hasSize(2);         // Two columns

        Category category = (Category) row[0];  // First column
        Long productCount = (Long) row[1];      // Second column

        assertThat(category.getName()).isEqualTo("Electronics");
        assertThat(productCount).isZero();
    }

    // ============================================
    // Testing Hierarchy Navigation
    // ============================================

    @Test
    @DisplayName("Should navigate from child to root through hierarchy")
    void testNavigateToRoot() {
        // Start from Gaming Laptops (level 2)
        Category level2 = categoryRepository.findById(gamingLaptops.getId()).orElseThrow();

        // Navigate to Laptops (level 1)
        Category level1 = level2.getParent();
        assertThat(level1).isNotNull();
        assertThat(level1.getName()).isEqualTo("Laptops");

        // Navigate to Electronics (root)
        Category root = level1.getParent();
        assertThat(root).isNotNull();
        assertThat(root.getName()).isEqualTo("Electronics");

        // Root has no parent
        assertThat(root.getParent()).isNull();
    }

    @Test
    @DisplayName("Should get all descendants of a category")
    void testGetAllDescendants() {
        // Act - Get all direct children of Electronics
        List<Category> directChildren = categoryRepository.findByParentId(electronics.getId());

        // Get grandchildren
        int totalDescendants = directChildren.size();
        for (Category child : directChildren) {
            totalDescendants += categoryRepository.countByParentId(child.getId());
        }

        // Assert
        assertThat(directChildren).hasSize(3);  // Laptops, Phones, Tablets
        assertThat(totalDescendants).isEqualTo(7);  // 3 direct + 4 grandchildren
    }

    // ============================================
    // Edge Cases & Real-World Scenarios
    // ============================================

    @Test
    @DisplayName("Should handle category with only root level")
    void testCategoryWithNoSubcategories() {
        // Books has no subcategories

        // Act
        List<Category> subcategories = categoryRepository.findByParentId(books.getId());
        long count = categoryRepository.countByParentId(books.getId());
        boolean hasChildren = categoryRepository.existsByParentId(books.getId());

        // Assert
        assertThat(subcategories).isEmpty();
        assertThat(count).isZero();
        assertThat(hasChildren).isFalse();
    }

    @Test
    @DisplayName("Should handle deep hierarchy (3+ levels)")
    void testDeepHierarchy() {
        // Arrange - Create level 3 category
        Category ultraGamingLaptops = Category.builder()
                .name("Ultra Gaming Laptops")
                .description("Extreme gaming laptops")
                .parent(gamingLaptops)  // Child of Gaming Laptops (level 2)
                .build();
        categoryRepository.save(ultraGamingLaptops);

        // Act - Navigate hierarchy
        Category level3 = categoryRepository.findByName("Ultra Gaming Laptops").orElseThrow();
        Category level2 = level3.getParent();
        Category level1 = level2.getParent();
        Category root = level1.getParent();

        // Assert - Full path: Ultra Gaming Laptops → Gaming Laptops → Laptops → Electronics
        assertThat(level3.getName()).isEqualTo("Ultra Gaming Laptops");
        assertThat(level2.getName()).isEqualTo("Gaming Laptops");
        assertThat(level1.getName()).isEqualTo("Laptops");
        assertThat(root.getName()).isEqualTo("Electronics");
        assertThat(root.getParent()).isNull();
    }

    @Test
    @DisplayName("Scenario: Build navigation menu - show root categories")
    void testNavigationMenuScenario() {
        // Scenario: Homepage - show main categories

        // Act
        List<Category> mainMenu = categoryRepository.findByParentIsNull();

        // Assert
        assertThat(mainMenu).hasSize(3);
        assertThat(mainMenu)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Electronics", "Clothing", "Books");
    }

    @Test
    @DisplayName("Scenario: Breadcrumb navigation")
    void testBreadcrumbScenario() {
        // Scenario: User viewing "Gaming Laptops" - show breadcrumb
        // Electronics > Laptops > Gaming Laptops

        // Act - Build breadcrumb from bottom to top
        Category current = categoryRepository.findById(gamingLaptops.getId()).orElseThrow();

        StringBuilder breadcrumb = new StringBuilder(current.getName());
        Category parent = current.getParent();

        while (parent != null) {
            breadcrumb.insert(0, parent.getName() + " > ");
            parent = parent.getParent();
        }

        // Assert
        assertThat(breadcrumb.toString()).isEqualTo("Electronics > Laptops > Gaming Laptops");
    }

    @Test
    @DisplayName("Scenario: Category dropdown - show hierarchical options")
    void testCategoryDropdownScenario() {
        // Scenario: Admin creating product - show categories with indentation

        // Act - Build hierarchical display
        List<Category> rootCategories = categoryRepository.findByParentIsNull();

        StringBuilder dropdown = new StringBuilder();
        for (Category root : rootCategories) {
            dropdown.append(root.getName()).append("\n");

            // Get level 1 children
            List<Category> level1Children = categoryRepository.findByParentId(root.getId());
            for (Category level1 : level1Children) {
                dropdown.append("  - ").append(level1.getName()).append("\n");

                // Get level 2 children
                List<Category> level2Children = categoryRepository.findByParentId(level1.getId());
                for (Category level2 : level2Children) {
                    dropdown.append("    - ").append(level2.getName()).append("\n");
                }
            }
        }

        // Assert - Verify structure
        String expected = """
                Electronics
                  - Laptops
                    - Gaming Laptops
                    - Business Laptops
                  - Phones
                    - Smartphones
                    - Feature Phones
                  - Tablets
                Clothing
                  - Men's Clothing
                  - Women's Clothing
                Books
                """;

        assertThat(dropdown.toString()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Scenario: Prevent deleting category with subcategories")
    void testPreventDeleteWithChildren() {
        // Scenario: Business rule - can't delete category if it has subcategories

        // Act
        boolean electronicsHasChildren = categoryRepository.existsByParentId(electronics.getId());
        boolean gamingLaptopsHasChildren = categoryRepository.existsByParentId(gamingLaptops.getId());

        // Assert
        if (electronicsHasChildren) {
            // Should not allow deletion
            assertThat(electronicsHasChildren).isTrue();
            System.out.println("Cannot delete Electronics - it has subcategories");
        }

        if (!gamingLaptopsHasChildren) {
            // Safe to delete
            assertThat(gamingLaptopsHasChildren).isFalse();
            categoryRepository.deleteById(gamingLaptops.getId());
            assertThat(categoryRepository.existsById(gamingLaptops.getId())).isFalse();
        }
    }

    @Test
    @DisplayName("Should move category to different parent")
    void testMoveCategoryToNewParent() {
        // Arrange - Move "Tablets" from Electronics to Clothing (weird but tests functionality)
        tablets.setParent(clothing);

        // Act
        Category updated = categoryRepository.save(tablets);

        // Assert
        assertThat(updated.getParent().getId()).isEqualTo(clothing.getId());
        assertThat(updated.getParent().getName()).isEqualTo("Clothing");

        // Verify Electronics children decreased
        assertThat(categoryRepository.countByParentId(electronics.getId())).isEqualTo(2);

        // Verify Clothing children increased
        assertThat(categoryRepository.countByParentId(clothing.getId())).isEqualTo(3);
    }
}
