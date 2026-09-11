package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCardResponse {
    private Long productId;
    private String maSo;
    private String tenHang;
    private String nhomHang;
    private String donViTinh;
    private List<StockCardEntryResponse> entries;
}
