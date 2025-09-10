package com.aegis.repository;

import com.aegis.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.QueryHint;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    @Query(value = """
        SELECT t FROM Transaction t 
        WHERE t.customerId = :customerId AND t.ts >= :from AND t.ts <= :to 
        ORDER BY t.ts DESC""",
        countQuery = """
        SELECT COUNT(t) FROM Transaction t 
        WHERE t.customerId = :customerId AND t.ts >= :from AND t.ts <= :to"""
    )
    @QueryHints({
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.comment", value = "Using customer_ts_idx")
    })
    Page<Transaction> findByCustomerIdAndTsBetweenOrderByTsDesc(
        @Param("customerId") String customerId, 
        @Param("from") OffsetDateTime from, 
        @Param("to") OffsetDateTime to, 
        Pageable pageable
    );

    @Query(value = """
        SELECT t FROM Transaction t 
        WHERE t.customerId = :customerId AND t.ts >= :from 
        ORDER BY t.ts DESC""",
        countQuery = """
        SELECT COUNT(t) FROM Transaction t 
        WHERE t.customerId = :customerId AND t.ts >= :from"""
    )
    @QueryHints({
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.comment", value = "Using customer_ts_idx")
    })
    Page<Transaction> findByCustomerIdAndTsAfterOrderByTsDesc(
        @Param("customerId") String customerId, 
        @Param("from") OffsetDateTime from, 
        Pageable pageable
    );

    @Query(value = """
        SELECT t FROM Transaction t 
        WHERE t.customerId = :customerId 
        ORDER BY t.ts DESC""",
        countQuery = """
        SELECT COUNT(t) FROM Transaction t 
        WHERE t.customerId = :customerId"""
    )
    @QueryHints({
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.comment", value = "Using customer_ts_idx")
    })
    Page<Transaction> findByCustomerIdOrderByTsDesc(
        @Param("customerId") String customerId, 
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant ORDER BY t.ts DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Transaction> findByMerchantOrderByTsDesc(@Param("merchant") String merchant);

    @Query("SELECT t FROM Transaction t WHERE t.mcc = :mcc ORDER BY t.ts DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Transaction> findByMccOrderByTsDesc(@Param("mcc") String mcc);

    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.customerId = :customerId AND t.status = :status 
        ORDER BY t.ts DESC""")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Transaction> findByCustomerIdAndStatusOrderByTsDesc(
        @Param("customerId") String customerId, 
        @Param("status") String status
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.customerId = :customerId AND t.ts >= :from")
    long countByCustomerIdAndTsAfter(
        @Param("customerId") String customerId, 
        @Param("from") OffsetDateTime from
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.customerId = :customerId AND t.ts >= :from")
    Long sumAmountByCustomerIdAndTsAfter(
        @Param("customerId") String customerId, 
        @Param("from") OffsetDateTime from
    );
}
