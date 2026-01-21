# Understanding Your Current Repositories & Implementing the Rest

## Part 1: What You Already Have

### UserRepository - Understanding the Code

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ============== AUTO-GENERATED METHODS (You get these for FREE) ==============
    // save(User user)                    - Create or Update
    // findById(Long id)                  - Find by primary key
    // findAll()                          - Get all users
    // findAll(Pageable pageable)         - Get users with pagination
    // deleteById(Long id)                - Delete by ID
    // delete(User user)                  - Delete entity
    // count()                            - Count total users
    // existsById(Long id)                - Check if user exists
    
    
    // ============== CUSTOM QUERY METHODS (You wrote these) ==============
    
    // SIMPLE FIND
    Optional<User> findByUsername(String username);
    // SQL: SELECT * FROM users WHERE username = ?
    
    Optional<User> findByEmail(String email);
    // SQL: SELECT * FROM users WHERE email = ?
    
    
    // FIND WITH MULTIPLE CONDITIONS (AND)
    Optional<User> findByUsernameAndEmail(String username, String email);
    // SQL: SELECT * FROM users WHERE username = ? AND email = ?
    
    
    // FIND BY ENUM STATUS
    List<User> findByStatus(UserStatus status);
    // SQL: SELECT * FROM users WHERE status = ?
    
    
    // FIND WITH LIKE (CASE INSENSITIVE)
    List<User> findByFirstNameContainingIgnoreCase(String firstName);
    // SQL: SELECT * FROM users WHERE LOWER(first_name) LIKE LOWER('%?%')
    
    
    // EXISTENCE CHECK (returns boolean, not entity)
    boolean existsByEmail(String email);
    // SQL: SELECT COUNT(*) > 0 FROM users WHERE email = ?
    // Use case: Check if email is already taken during registration
    
    boolean existsByUsername(String username);
    // SQL: SELECT COUNT(*) > 0 FROM users WHERE username = ?
    // Use case: Check if username is already taken
    
    
    // COUNT QUERY
    long countByStatus(UserStatus status);
    // SQL: SELECT COUNT(*) FROM users WHERE status = ?
    // Use case: Dashboard showing "Active Users: 150"
    
    
    // DELETE QUERY (returns count of deleted records)
    long deleteByStatus(UserStatus status);
    // SQL: DELETE FROM users WHERE status = ?
    // Use case: Cleanup inactive/deleted accounts
    
    
    // SORTING
    List<User> findByStatusOrderByCreatedAtDesc(UserStatus status);
    // SQL: SELECT * FROM users WHERE status = ? ORDER BY created_at DESC
    // Returns newest users first
    
    
    // PAGINATION
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    // Use case: Show active users, 20 per page
    // Call: userRepository.findByStatus(ACTIVE, PageRequest.of(0, 20))
}
```

**Key Takeaways for UserRepository:**
- 8 auto-generated methods for basic CRUD
- `existsBy` methods are FASTER than `findBy` when you only need to check existence
- `deleteBy` returns count of deleted records
- Pagination prevents loading 10,000 users into memory at once

---

### ProductRepository - Understanding the Code

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // ============== YOUR CUSTOM METHODS ==============
    
    Optional<Product> findBySku(String sku);
    // Use case: Look up product by SKU barcode
    
    List<Product> findByActiveTrue();
    // SQL: SELECT * FROM products WHERE active = true
    // Note: Spring understands "True" as = true
    
    List<Product> findByCategoryId(Long categoryId);
    // SQL: SELECT * FROM products WHERE category_id = ?
    // Navigates relationship: product.category.id
    
    
    // RANGE QUERIES
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    // SQL: SELECT * FROM products WHERE price BETWEEN ? AND ?
    // Use case: Filter products $50-$100
    
    
    // COMPARISON QUERIES
    List<Product> findByStockQuantityLessThan(Integer quantity);
    // SQL: SELECT * FROM products WHERE stock_quantity < ?
    // Use case: Low stock alert - find products with < 10 items
    
    
    // COMBINED CONDITIONS
    List<Product> findByActiveTrueAndStockQuantityGreaterThan(Integer quantity);
    // SQL: SELECT * FROM products WHERE active = true AND stock_quantity > ?
    // Use case: Only show products that are active AND in stock
    
    
    // MULTIPLE FIELD SEARCH (OR)
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String name, String description);
    // SQL: SELECT * FROM products WHERE 
    //      LOWER(name) LIKE LOWER('%?%') OR LOWER(description) LIKE LOWER('%?%')
    // Use case: Search box - searches both name and description
    
    
    // PAGINATION WITH FILTERS
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    // Use case: Show active products in "Electronics" category, 20 per page
}
```

**Key Takeaways for ProductRepository:**
- `Between` for range queries (price, dates)
- `LessThan`, `GreaterThan` for comparisons
- `And` / `Or` for combining conditions
- Nested property access: `findByCategoryId` accesses `product.category.id`

---

### OrderRepository - Understanding the Code

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    // Use case: Track order by confirmation number
    
    List<Order> findByUserId(Long userId);
    // SQL: SELECT * FROM orders WHERE user_id = ?
    // Use case: Show all orders for a customer
    
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    // Use case: Show only "DELIVERED" orders for a customer
    
    
    // DATE RANGE
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
    // Use case: Sales report for January 2024
    
    
    // TOP N RESULTS
    List<Order> findTop10ByUserIdOrderByOrderDateDesc(Long userId);
    // SQL: SELECT * FROM orders WHERE user_id = ? 
    //      ORDER BY order_date DESC LIMIT 10
    // Use case: Show customer's 10 most recent orders
    
    
    long countByUserIdAndStatus(Long userId, OrderStatus status);
    // Use case: "You have 3 pending orders"
    
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    // Use case: Customer order history with pagination
}
```

**Key Takeaways for OrderRepository:**
- `TopN` for limiting results (alternative to pagination)
- `OrderBy` for sorting in method name
- Date range queries with `Between`

---

## Part 2: Query Method Naming Conventions

### The Pattern: `findBy + Property + Condition + OrderBy + Sorting`

| Keyword | Example | SQL |
|---------|---------|-----|
| `And` | findByNameAndPrice | WHERE name = ? AND price = ? |
| `Or` | findByNameOrPrice | WHERE name = ? OR price = ? |
| `Between` | findByPriceBetween | WHERE price BETWEEN ? AND ? |
| `LessThan` | findByPriceLessThan | WHERE price < ? |
| `GreaterThan` | findByPriceGreaterThan | WHERE price > ? |
| `Like` | findByNameLike | WHERE name LIKE ? |
| `Containing` | findByNameContaining | WHERE name LIKE '%?%' |
| `StartingWith` | findByNameStartingWith | WHERE name LIKE '?%' |
| `EndingWith` | findByNameEndingWith | WHERE name LIKE '%?' |
| `IgnoreCase` | findByNameIgnoreCase | WHERE LOWER(name) = LOWER(?) |
| `OrderBy` | findByStatusOrderByCreatedAtDesc | ORDER BY created_at DESC |
| `Top` / `First` | findTop5ByOrderByPriceDesc | LIMIT 5 |
| `Distinct` | findDistinctByStatus | SELECT DISTINCT |

---

## Part 3: Implementing Remaining Repositories

Now let's implement the rest, one by one with explanations.

---

### 1. AddressRepository (Simple)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.Address;
import com.ecommerce.entity.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    // Find all addresses for a user
    List<Address> findByUserId(Long userId);
    // Use case: Show all saved addresses in checkout
    
    // Find addresses by type for a user
    List<Address> findByUserIdAndType(Long userId, AddressType type);
    // Use case: Get all "SHIPPING" addresses for a user
    
    // Find default address for a user
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    // Use case: Pre-select default shipping address in checkout
    
    // Find addresses in a city
    List<Address> findByCity(String city);
    // Use case: Analytics - how many customers in "Toronto"
    
    // Count addresses per user
    long countByUserId(Long userId);
    // Use case: Limit users to max 5 addresses
    
    // Delete all addresses for a user
    long deleteByUserId(Long userId);
    // Use case: When deleting a user account
    
    // Custom query: Find addresses by postal code prefix (Canada specific)
    @Query("SELECT a FROM Address a WHERE a.postalCode LIKE :prefix%")
    List<Address> findByPostalCodePrefix(@Param("prefix") String prefix);
    // Use case: Find all addresses in postal code area "M5G" (downtown Toronto)
}
```

**Why AddressRepository is Simple:**
- Mostly basic queries
- One custom query for postal code search
- Good for learning query method naming

---

### 2. RoleRepository (Simplest)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // Find role by name
    Optional<Role> findByName(RoleType name);
    // Use case: Get ROLE_ADMIN when assigning admin privileges
    
    // Check if role exists
    boolean existsByName(RoleType name);
    // Use case: Ensure default roles exist during app startup
}
```

**Why RoleRepository is Simplest:**
- Roles are mostly static (ADMIN, USER, SELLER)
- Just need basic lookup
- No complex queries needed

---

### 3. CategoryRepository (Self-Referencing)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Find category by name
    Optional<Category> findByName(String name);
    // Use case: Lookup "Electronics" category
    
    // Find all root categories (top-level, no parent)
    List<Category> findByParentIsNull();
    // SQL: SELECT * FROM categories WHERE parent_id IS NULL
    // Use case: Display main navigation menu
    
    // Find subcategories of a parent
    List<Category> findByParentId(Long parentId);
    // SQL: SELECT * FROM categories WHERE parent_id = ?
    // Use case: When user clicks "Electronics", show "Laptops", "Phones", etc.
    
    // Count subcategories
    long countByParentId(Long parentId);
    // Use case: Show "Electronics (5)" indicating 5 subcategories
    
    // Check if category has subcategories
    boolean existsByParentId(Long parentId);
    // Use case: Prevent deleting categories with subcategories
    
    // Custom query: Find category with products count
    @Query("SELECT c, COUNT(p) FROM Category c " +
           "LEFT JOIN c.products p " +
           "WHERE c.id = :categoryId " +
           "GROUP BY c")
    Object[] findCategoryWithProductCount(@Param("categoryId") Long categoryId);
    // Use case: Display "Laptops (42 products)"
    
    // Find categories by name containing keyword (search)
    List<Category> findByNameContainingIgnoreCase(String keyword);
    // Use case: Search categories
}
```

**Why CategoryRepository is Interesting:**
- Self-referencing relationship (`parent_id`)
- `IsNull` for finding root categories
- Good example of hierarchical data

---

### 4. ReviewRepository (Many-to-Many Junction)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Find all reviews for a product
    List<Review> findByProductId(Long productId);
    // Use case: Show all reviews on product page
    
    // Find reviews with pagination (for products with 1000s of reviews)
    Page<Review> findByProductId(Long productId, Pageable pageable);
    // Use case: Show 10 reviews per page
    
    // Find reviews by a user
    List<Review> findByUserId(Long userId);
    // Use case: Show "My Reviews" page
    
    // Find verified purchase reviews only
    List<Review> findByProductIdAndVerifiedPurchaseTrue(Long productId);
    // Use case: Filter to show only verified reviews
    
    // Find reviews by rating
    List<Review> findByProductIdAndRating(Long productId, Integer rating);
    // Use case: Filter "5-star reviews only"
    
    // Find reviews with rating >= threshold
    List<Review> findByProductIdAndRatingGreaterThanEqual(Long productId, Integer rating);
    // Use case: Show all reviews rated 4 or 5 stars
    
    // Check if user already reviewed a product
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    // Use case: Prevent duplicate reviews from same user
    
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    // Use case: Load user's existing review for editing
    
    // Custom query: Average rating for a product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
    // Use case: Display "4.5 stars" on product
    
    // Custom query: Rating distribution
    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
           "WHERE r.product.id = :productId " +
           "GROUP BY r.rating " +
           "ORDER BY r.rating DESC")
    List<Object[]> findRatingDistribution(@Param("productId") Long productId);
    // Use case: Show bar chart "5★: 45 reviews, 4★: 23 reviews..."
    
    // Count reviews per product
    long countByProductId(Long productId);
    // Use case: Display "42 reviews"
    
    // Delete all reviews for a product
    long deleteByProductId(Long productId);
    // Use case: When deleting a product
}
```

**Why ReviewRepository is Important:**
- Demonstrates aggregation queries (AVG, COUNT)
- GROUP BY for rating distribution
- Checking duplicate reviews
- Pagination for large datasets

---

### 5. OrderItemRepository (Junction Table)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Find all items in an order
    List<OrderItem> findByOrderId(Long orderId);
    // Use case: Display order details with all items
    
    // Find all orders containing a specific product
    List<OrderItem> findByProductId(Long productId);
    // Use case: See who bought this product
    
    // Count how many times a product was ordered
    long countByProductId(Long productId);
    // Use case: "Bought by 1,234 customers"
    
    // Custom query: Find best-selling products
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.status = 'DELIVERED' " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts();
    // Use case: Dashboard showing top 10 products
    
    // Custom query: Total revenue for a product
    @Query("SELECT SUM(oi.unitPrice * oi.quantity) FROM OrderItem oi " +
           "WHERE oi.product.id = :productId " +
           "AND oi.order.status = 'DELIVERED'")
    BigDecimal calculateProductRevenue(@Param("productId") Long productId);
    // Use case: Product analytics
    
    // Custom query: Find products purchased together (co-purchase analysis)
    @Query("SELECT oi2.product.id, oi2.product.name, COUNT(oi2) as frequency " +
           "FROM OrderItem oi1 " +
           "JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id " +
           "WHERE oi1.product.id = :productId " +
           "AND oi2.product.id != :productId " +
           "GROUP BY oi2.product.id, oi2.product.name " +
           "ORDER BY frequency DESC")
    List<Object[]> findFrequentlyBoughtTogether(@Param("productId") Long productId);
    // Use case: "Customers who bought this also bought..."
}
```

**Why OrderItemRepository is Advanced:**
- Aggregation (SUM, COUNT)
- JOIN across multiple tables
- Business analytics queries
- Co-purchase analysis (Amazon-style recommendations)

---

### 6. PaymentRepository (One-to-One)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.Payment;
import com.ecommerce.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Find payment by order ID
    Optional<Payment> findByOrderId(Long orderId);
    // Use case: Get payment details for an order
    
    // Find payment by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
    // Use case: Lookup payment from webhook callback
    
    // Find all payments with a status
    List<Payment> findByStatus(PaymentStatus status);
    // Use case: Find all "PENDING" payments to retry
    
    // Find payments in date range
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    // Use case: Daily payment report
    
    // Find failed payments for retry
    List<Payment> findByStatusAndPaymentDateBefore(
        PaymentStatus status, 
        LocalDateTime cutoffDate
    );
    // Use case: Find payments that failed > 1 hour ago to retry
    
    // Count payments by status
    long countByStatus(PaymentStatus status);
    // Use case: Dashboard - "5 failed payments today"
    
    // Custom query: Total revenue
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal calculateRevenue(
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end
    );
    // Use case: Monthly revenue report
    
    // Custom query: Revenue by payment method
    @Query("SELECT p.paymentMethod, SUM(p.amount) " +
           "FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "GROUP BY p.paymentMethod")
    List<Object[]> getRevenueByPaymentMethod();
    // Use case: See how much came from credit cards vs PayPal
}
```

**Why PaymentRepository is Business-Critical:**
- Financial data - needs accuracy
- Status tracking for payment lifecycle
- Revenue reporting
- Retry logic for failed payments

---

### 7. ProductImageRepository (Simple)

```java
package com.ecommerce.repository;

import com.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    // Find all images for a product
    List<ProductImage> findByProductId(Long productId);
    // Use case: Load product gallery
    
    // Find images sorted by order
    List<ProductImage> findByProductIdOrderBySortOrder(Long productId);
    // Use case: Display images in specific order
    
    // Find primary image
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);
    // Use case: Get thumbnail for product listing
    
    // Count images for a product
    long countByProductId(Long productId);
    // Use case: Limit products to max 10 images
    
    // Delete all images for a product
    long deleteByProductId(Long productId);
    // Use case: When deleting a product
}
```

**Why ProductImageRepository is Simple:**
- Straightforward queries
- Mostly lookup and sorting
- Good for practicing OrderBy

---

## Summary: Repository Complexity Levels

### Level 1 - Simple (Start Here)
- ✅ **RoleRepository** - Just lookups
- ✅ **AddressRepository** - Basic queries
- ✅ **ProductImageRepository** - Simple relationships

### Level 2 - Intermediate
- ✅ **CategoryRepository** - Self-referencing
- ✅ **ReviewRepository** - Aggregations

### Level 3 - Advanced
- ✅ **OrderItemRepository** - Complex joins, analytics
- ✅ **PaymentRepository** - Business logic

---

## Next Steps

1. **Copy these repository interfaces into your project**
2. **Test them one by one** - I'll show you how
3. **Learn the query method patterns** - they repeat across all repos
4. **Ask about any method you don't understand**

Which repository would you like to implement and test first? I recommend starting with **RoleRepository** since it's the simplest!