package saigonbank.fit.ims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Hàng hóa: maSo là duy nhất; không thể xóa nếu đã có phát sinh nhập/xuất
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_so", nullable = false, unique = true, length = 50)
    private String maSo;

    @Column(name = "ten_hang", nullable = false, length = 300)
    private String tenHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_group_id", nullable = false)
    private ProductGroup productGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "don_gia_nhap", nullable = false, precision = 18)
    @Builder.Default
    private BigDecimal donGiaNhap = BigDecimal.ZERO;

    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = ProductStatus.HOAT_DONG;

    public static final class ProductStatus {
        public static final String HOAT_DONG      = "HOAT_DONG";
        public static final String NGUNG_SU_DUNG  = "NGUNG_SU_DUNG";
        private ProductStatus() {}
    }

    public String getNhomHangCode() {
        return productGroup != null ? productGroup.getCode() : null;
    }

    public String getDonViTinh() {
        return unit != null ? unit.getName() : null;
    }

    public boolean isActive() {
        return ProductStatus.HOAT_DONG.equals(trangThai);
    }
}
