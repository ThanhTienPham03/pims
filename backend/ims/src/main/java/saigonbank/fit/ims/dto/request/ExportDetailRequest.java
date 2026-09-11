package saigonbank.fit.ims.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExportDetailRequest {

    @NotNull(message = "Product ID là bắt buộc")
    private Long productId;

    @NotNull(message = "Số lượng là bắt buộc")
    @Positive(message = "Số lượng phải > 0")
    private BigDecimal quantity;
}
