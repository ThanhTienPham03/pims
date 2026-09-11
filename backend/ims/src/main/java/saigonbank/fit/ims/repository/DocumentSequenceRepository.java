package saigonbank.fit.ims.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import saigonbank.fit.ims.entity.DocumentSequence;

import java.util.Optional;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ds FROM DocumentSequence ds
        WHERE ds.year = :year AND ds.groupCode = :groupCode AND ds.docType = :docType
    """)
    Optional<DocumentSequence> findForUpdate(
            @Param("year") Integer year,
            @Param("groupCode") String groupCode,
            @Param("docType") String docType
    );

    Optional<DocumentSequence> findByYearAndGroupCodeAndDocType(Integer year, String groupCode, String docType);

    // Atomic INSERT IGNORE tránh race condition khi tạo mới sequence
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO document_sequences (year, group_code, doc_type, last_sequence)
        VALUES (:year, :groupCode, :docType, 0)
    """, nativeQuery = true)
    void insertIgnore(
            @Param("year") Integer year,
            @Param("groupCode") String groupCode,
            @Param("docType") String docType
    );
}
