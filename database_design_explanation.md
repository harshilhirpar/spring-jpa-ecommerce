# E-Commerce Database Design - Thinking & ER Diagram

## Design Philosophy

The e-commerce database is designed around **4 core domains**:

1. **User Management** - Who uses the system
2. **Product Catalog** - What we sell
3. **Order Processing** - Transactions and purchases
4. **Supporting Data** - Reviews, addresses, etc.

---

## Entity Design Thinking

### 1. User Domain

**User Entity** (Central entity for authentication & identity)
- Stores core user information (username, email, password)
- Has a status to track account state (active, suspended, etc.)
- Uses roles for authorization (admin, customer, seller)

**Why separate Address from User?**
- Users can have multiple addresses (home, work, shipping, billing)
- Same address might be used across multiple orders
- Allows updating user info without affecting historical orders

**Why Many-to-Many with Role?**
- A user can have multiple roles (customer + seller)
- Same role is shared by many users
- More flexible than storing role as enum in User table

---

### 2. Product Catalog Domain

**Category Entity** (Product organization)
- Self-referencing relationship for hierarchy
- Example: Electronics → Laptops → Gaming Laptops
- Parent category can have multiple subcategories

**Product Entity** (What we sell)
- Belongs to ONE category (Many-to-One)
- Has SKU for inventory tracking
- Tracks stock quantity for availability
- Supports discount pricing

**ProductImage Entity** (Visual representation)
- One product can have multiple images
- Marked as primary for main display
- Sort order for gallery display

**Why separate ProductImage?**
- Products can have 0 to many images
- Easier to manage image CDN URLs
- Can add/remove images without modifying Product

---

### 3. Order Processing Domain

**Order Entity** (Transaction record)
- Belongs to ONE user (who placed it)
- Has unique order number for tracking
- Tracks status through fulfillment lifecycle
- Stores total amount for reporting

**OrderItem Entity** (Order line items)
- Junction between Order and Product
- Each item stores quantity and price AT TIME OF PURCHASE
- Captures historical pricing (product price might change later)

**Why store price in OrderItem?**
- Product prices change over time
- Need historical accuracy for accounting
- Order should reflect what customer actually paid

**Payment Entity** (Financial transaction)
- One-to-One with Order
- Stores payment method and transaction ID
- Separate status from order (payment can fail while order exists)

**Why One-to-One with Order?**
- Each order has exactly one payment
- Keeps order entity clean
- Payment details are sensitive, easier to secure separately

---

### 4. Supporting Domain

**Review Entity** (Product feedback)
- Links User to Product
- Many users can review many products
- Tracks verified purchase flag
- Rating + comment for feedback

**Why not Many-to-Many direct?**
- Need extra attributes (rating, comment, date)
- User can only review same product once
- Verified purchase flag requires order context

---

## Key Design Decisions

### 1. BaseEntity Pattern
All entities extend BaseEntity which provides:
- `createdAt` - when record was created
- `updatedAt` - when record was last modified
- `createdBy` - who created it (auditing)
- `lastModifiedBy` - who modified it (auditing)
- `version` - for optimistic locking (prevents concurrent update conflicts)

**Why?** Standardizes auditing and prevents lost updates in concurrent scenarios.

### 2. Bidirectional Relationships
Example: User ↔ Order
- User has `List<Order> orders`
- Order has `User user`

**Benefits:**
- Navigate from User to their Orders
- Navigate from Order back to User
- Helper methods maintain consistency

**Trade-off:** More complex to manage, but worth it for convenience.

### 3. Cascade Types
- `CascadeType.ALL` on User → Address (delete user = delete addresses)
- `CascadeType.ALL` on Order → OrderItem (delete order = delete items)
- No cascade on Product → OrderItem (deleting product shouldn't delete orders)

**Principle:** Cascade on "owns" relationships, not on "references".

### 4. FetchType Strategy
- Default to `LAZY` for associations
- Use `EAGER` only for small, frequently needed data (like User → Roles)
- Use JOIN FETCH in queries when you need data

**Why?** Prevents loading entire database in memory (N+1 problem).

---

## ER Diagram

```
┌─────────────────┐         ┌─────────────────┐
│      User       │────────<│    Address      │
│─────────────────│  1    * │─────────────────│
│ PK: id          │         │ PK: id          │
│    username     │         │ FK: user_id     │
│    email        │         │    street       │
│    password     │         │    city         │
│    firstName    │         │    state        │
│    lastName     │         │    postalCode   │
│    phoneNumber  │         │    country      │
│    status       │         │    type         │
└────────┬────────┘         │    isDefault    │
         │                  └─────────────────┘
         │ *
         │
         │ *          ┌─────────────────┐
         └───────────<│      Role       │
          user_roles  │─────────────────│
              *       │ PK: id          │
                      │    name         │
                      │    description  │
                      └─────────────────┘


┌─────────────────┐         ┌─────────────────┐
│    Category     │────────<│    Product      │
│─────────────────│  1    * │─────────────────│
│ PK: id          │         │ PK: id          │
│    name         │         │ FK: category_id │
│    description  │         │    sku          │
│ FK: parent_id   │◄──┐     │    name         │
└─────────────────┘   │     │    description  │
    (self-ref)        │     │    price        │
                      │     │    discountPrice│
                      │     │    stockQuantity│
                      └─────│    active       │
                            └────────┬────────┘
                                     │ 1
                                     │
                    ┌────────────────┼────────────────┐
                    │ *              │ *              │ *
                    │                │                │
         ┌──────────▼──────┐  ┌──────▼──────┐  ┌─────▼─────────┐
         │ ProductImage    │  │   Review    │  │  OrderItem    │
         │─────────────────│  │─────────────│  │───────────────│
         │ PK: id          │  │ PK: id      │  │ PK: id        │
         │ FK: product_id  │  │ FK: prod_id │  │ FK: order_id  │
         │    imageUrl     │  │ FK: user_id │  │ FK: product_id│
         │    isPrimary    │  │    rating   │  │    quantity   │
         │    sortOrder    │  │    comment  │  │    unitPrice  │
         └─────────────────┘  │    verified │  │    discount   │
                              └─────────────┘  └───────┬───────┘
                                                       │ *
                                                       │
                                                       │ 1
                              ┌────────────────────────▼───────┐
┌─────────────────┐           │         Order                  │
│    Payment      │────1:1───<│────────────────────────────────│
│─────────────────│           │ PK: id                         │
│ PK: id          │           │ FK: user_id                    │
│ FK: order_id    │           │ FK: shipping_address_id        │
│    transactionId│           │    orderNumber                 │
│    method       │           │    orderDate                   │
│    status       │           │    status                      │
│    amount       │           │    totalAmount                 │
│    paymentDate  │           │    shippingFee                 │
└─────────────────┘           │    taxAmount                   │
                              └────────────────────────────────┘
                                            ▲
                                            │ *
                                            │ 1
                                      ┌─────┴──────┐
                                      │    User    │
                                      └────────────┘
```

---

## Relationship Summary

| Entity 1 | Relationship | Entity 2 | Type | Why? |
|----------|--------------|----------|------|------|
| User | 1:M | Address | OneToMany | User has multiple addresses |
| User | M:N | Role | ManyToMany | User can have multiple roles |
| User | 1:M | Order | OneToMany | User places multiple orders |
| User | 1:M | Review | OneToMany | User writes multiple reviews |
| Category | 1:M | Product | OneToMany | Category contains multiple products |
| Category | 1:M | Category | OneToMany (self) | Category hierarchy |
| Product | 1:M | ProductImage | OneToMany | Product has multiple images |
| Product | 1:M | Review | OneToMany | Product has multiple reviews |
| Product | 1:M | OrderItem | OneToMany | Product appears in many orders |
| Order | 1:M | OrderItem | OneToMany | Order contains multiple items |
| Order | 1:1 | Payment | OneToOne | Order has one payment |
| Order | M:1 | Address | ManyToOne | Order ships to one address |

---

## Database Normalization

The design follows **3rd Normal Form (3NF)**:

✅ **1NF**: No repeating groups, atomic values
✅ **2NF**: No partial dependencies (all non-key attributes depend on whole primary key)
✅ **3NF**: No transitive dependencies (non-key attributes don't depend on other non-key attributes)

**Example of good normalization:**
- Address is separate (not duplicated in User and Order)
- OrderItem stores price snapshot (not computed from Product every time)
- Category hierarchy uses self-reference (not separate tables per level)

---

## What You Should Focus On Next

Now that you have entities and 3 repositories, here's the progression:

1. **Master Repository Basics** (UserRepository, ProductRepository, OrderRepository)
    - Derived query methods
    - Understand naming conventions

2. **Add Custom Queries**
    - JPQL for complex joins
    - Learn when to use JPQL vs derived queries

3. **Implement Service Layer**
    - Business logic with transactions
    - Orchestrate multiple repositories

4. **Add Specifications** (for dynamic search)
    - Build flexible search features

Which 3 repositories have you implemented? I'll help you understand them better and add the next ones step by step.