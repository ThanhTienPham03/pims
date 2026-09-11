package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCardEntryResponse {
    private Long id;
    private LocalDate entryDate;
    private String soPhieu;
    private String tranType;

    // Nhập
    private BigDecimal importQuantity;
    private BigDecimal importUnitPrice;
    private BigDecimal importAmount;

    // Xuất
    private BigDecimal exportQuantity;
    private BigDecimal exportUnitPrice;
    private BigDecimal exportAmount;

    // Tồn
    private BigDecimal balanceQuantity;
    private BigDecimal balanceAmount;

    private String note;
    private String createdBy;
}
