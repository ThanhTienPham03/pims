package saigonbank.fit.ims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Quản lý STT sinh số chứng từ theo định dạng STT/GROUP-YYYY (pessimistic lock)
@Entity
@Table(name = "document_sequences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"year", "group_code", "doc_type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "group_code", nullable = false, length = 10)
    private String groupCode;

    @Column(name = "doc_type", nullable = false, length = 20)
    @Builder.Default
    private String docType = "EXPORT";

    @Column(name = "last_sequence", nullable = false)
    @Builder.Default
    private Integer lastSequence = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    public int nextSequence() {
        this.lastSequence = this.lastSequence + 1;
        return this.lastSequence;
    }

    public String formatDocumentNumber() {
        return lastSequence + "/" + groupCode + "-" + year;
    }
}
