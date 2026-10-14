package saigonbank.fit.ims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified Inventory Transaction document (Phiếu giao dịch).
 * Replaces both Import and Export documents to optimize database schema.
 */
@Entity
@Table(
    name = "inventory_transactions",
    uniqueConstraints = @UniqueConstraint(name = "uq_tx_type_code", columnNames = {"type", "transaction_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", nullable = false, length = 50)
    private String transactionCode; // so_phieu

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate; // ngay_nhap / ngay_xuat

    @Column(name = "source", length = 300)
    private String source; // noi_mua (for IMPORT)

    @Column(name = "destination", length = 300)
    private String destination; // noi_nhap / noi_nhan (for IMPORT/EXPORT)

    @Column(name = "reason", length = 500)
    private String reason; // ly_do_xuat

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber; // so_hoa_don

    @Column(name = "invoice_date")
    private LocalDate invoiceDate; // ngay_hoa_don

    @Column(name = "debit_account", length = 20)
    private String debitAccount; // tk_no

    @Column(name = "credit_account", length = 20)
    private String creditAccount; // tk_co

    @Column(name = "total_amount", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO; // tong_tien

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = TransactionStatus.HOAN_THANH; // trang_thai

    @Column(name = "group_representative", length = 10)
    private String groupRepresentative; // nhom_dai_dien

    @Column(name = "workflow_key", length = 100)
    private String workflowKey;

    @Column(name = "workflow_status", length = 20)
    @Builder.Default
    private String workflowStatus = "COMPLETED";

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InventoryTransactionDetail> details = new ArrayList<>();

    public enum TransactionType {
        IMPORT,
        EXPORT,
        ADJUSTMENT,
        CHECK
    }

    public static final class TransactionStatus {
        public static final String HOAN_THANH = "HOAN_THANH";
        public static final String HUY        = "HUY";
        private TransactionStatus() {}
    }
}
