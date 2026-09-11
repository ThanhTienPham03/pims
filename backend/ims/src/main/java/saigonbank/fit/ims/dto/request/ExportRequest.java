package saigonbank.fit.ims.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ExportRequest {

    @NotNull(message = "Ngày xuất là bắt buộc")
    private LocalDate ngayXuat;

    private String noiNhan;

    private String lyDoXuat;

    private String tkNo;

    private String tkCo;

    @NotEmpty(message = "Phiếu xuất phải có ít nhất một dòng hàng hóa")
    @Valid
    private List<ExportDetailRequest> details;
}
