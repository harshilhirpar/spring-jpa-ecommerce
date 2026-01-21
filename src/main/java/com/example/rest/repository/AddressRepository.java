package com.example.rest.repository;

import com.example.rest.entity.Address;
import com.example.rest.entity.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

//    Find All address for a user
    List<Address> findByUserId(Long userId);

    List<Address> findByUserIdAndType(Long userId, AddressType type);

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
