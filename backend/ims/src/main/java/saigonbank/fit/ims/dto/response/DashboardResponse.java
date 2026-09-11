package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    /** Số sản phẩm đang hoạt động */
    private long totalActiveProducts;
    /** Số sản phẩm có tồn kho > 0 */
    private long productsWithStock;
    /** Tổng giá trị tồn kho hiện tại (VND) */
    private BigDecimal totalInventoryAmount;
    /** Số phiếu nhập trong tháng hiện tại */
    private long importsThisMonth;
    /** Số phiếu xuất trong tháng hiện tại */
    private long exportsThisMonth;
    /** Tháng/năm hiện tại */
    private int currentMonth;
    private int currentYear;
}
