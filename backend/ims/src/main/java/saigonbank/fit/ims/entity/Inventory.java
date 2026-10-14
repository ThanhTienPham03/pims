package saigonbank.fit.ims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tồn kho hiện tại: ending = opening + import - export; averagePrice = endingAmount / endingQuantity
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "opening_quantity", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal openingQuantity = BigDecimal.ZERO;

    @Column(name = "opening_amount", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal openingAmount = BigDecimal.ZERO;

    @Column(name = "import_quantity", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal importQuantity = BigDecimal.ZERO;

    @Column(name = "import_amount", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal importAmount = BigDecimal.ZERO;

    @Column(name = "export_quantity", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal exportQuantity = BigDecimal.ZERO;

    @Column(name = "export_amount", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal exportAmount = BigDecimal.ZERO;

    @Column(name = "ending_quantity", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal endingQuantity = BigDecimal.ZERO;

    @Column(name = "ending_amount", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal endingAmount = BigDecimal.ZERO;

    @Column(name = "average_price", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal averagePrice = BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public void recalculate() {
        this.endingQuantity = openingQuantity.add(importQuantity).subtract(exportQuantity);
        this.endingAmount   = openingAmount.add(importAmount).subtract(exportAmount);

        if (this.endingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePrice = this.endingAmount
                    .divide(this.endingQuantity, 0, java.math.RoundingMode.HALF_UP);
        } else {
            this.averagePrice = BigDecimal.ZERO;
            this.endingAmount = BigDecimal.ZERO;
        }
    }

    public void applyImport(BigDecimal qty, BigDecimal amount) {
        this.importQuantity = this.importQuantity.add(qty);
        this.importAmount   = this.importAmount.add(amount);
        recalculate();
    }

    public void applyExport(BigDecimal qty, BigDecimal exportAmount) {
        if (this.endingQuantity.compareTo(qty) == 0) {
            // Xuất toàn bộ tồn: dùng endingAmount thực tế để triệt tiêu lệch số học
            BigDecimal remainingAmount = this.endingAmount;
            this.exportQuantity = this.exportQuantity.add(qty);
            this.exportAmount   = this.exportAmount.add(remainingAmount);
        } else {
            this.exportQuantity = this.exportQuantity.add(qty);
            this.exportAmount   = this.exportAmount.add(exportAmount);
        }
        recalculate();
    }

    public void reverseImport(BigDecimal qty, BigDecimal amount) {
        this.importQuantity = this.importQuantity.subtract(qty);
        this.importAmount   = this.importAmount.subtract(amount);
        recalculate();
    }

    public void reverseExport(BigDecimal qty, BigDecimal amount) {
        this.exportQuantity = this.exportQuantity.subtract(qty);
        this.exportAmount   = this.exportAmount.subtract(amount);
        recalculate();
    }
}
