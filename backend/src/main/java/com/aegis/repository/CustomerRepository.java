package com.aegis.repository;

import com.aegis.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    @Query("SELECT c FROM Customer c WHERE c.status = 'active'")
    List<Customer> findAllActive();
    
    @Query("SELECT c FROM Customer c WHERE c.id = :id AND c.status = 'active'")
    Optional<Customer> findActiveById(@Param("id") String id);
    
    // @Query("SELECT c FROM Customer c WHERE c.riskFlags IS NOT NULL AND cardinality(c.riskFlags) > 0")
    // List<Customer> findCustomersWithRiskFlags();
}
