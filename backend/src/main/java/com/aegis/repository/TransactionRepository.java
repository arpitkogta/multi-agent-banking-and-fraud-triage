package com.aegis.repository;

import com.aegis.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId ORDER BY t.ts DESC")
    Page<Transaction> findByCustomerIdOrderByTsDesc(@Param("customerId") String customerId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId AND t.ts >= :from AND t.ts <= :to ORDER BY t.ts DESC")
    Page<Transaction> findByCustomerIdAndTsBetweenOrderByTsDesc(
        @Param("customerId") String customerId, 
        @Param("from") OffsetDateTime from, 
        @Param("to") OffsetDateTime to, 
        Pageable pageable
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId AND t.ts >= :from ORDER BY t.ts DESC")
    Page<Transaction> findByCustomerIdAndTsAfterOrderByTsDesc(
        @Param("customerId") String customerId, 
        @Param("from") OffsetDateTime from, 
        Pageable pageable
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant ORDER BY t.ts DESC")
    List<Transaction> findByMerchantOrderByTsDesc(@Param("merchant") String merchant);
    
    @Query("SELECT t FROM Transaction t WHERE t.mcc = :mcc ORDER BY t.ts DESC")
    List<Transaction> findByMccOrderByTsDesc(@Param("mcc") String mcc);
    
    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId AND t.status = :status ORDER BY t.ts DESC")
    List<Transaction> findByCustomerIdAndStatusOrderByTsDesc(@Param("customerId") String customerId, @Param("status") String status);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.customerId = :customerId AND t.ts >= :from")
    long countByCustomerIdAndTsAfter(@Param("customerId") String customerId, @Param("from") OffsetDateTime from);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.customerId = :customerId AND t.ts >= :from")
    Long sumAmountByCustomerIdAndTsAfter(@Param("customerId") String customerId, @Param("from") OffsetDateTime from);
}
