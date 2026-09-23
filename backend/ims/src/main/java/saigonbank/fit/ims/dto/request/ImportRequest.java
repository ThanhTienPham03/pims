package saigonbank.fit.ims.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class ImportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Ngày nhập là bắt buộc")
    private LocalDate ngayNhap;

    private String noiNhap;

    private String noiMua;

    private String soHoaDon;

    private LocalDate ngayHoaDon;

    private String tkNo;

    private String tkCo;

    @NotEmpty(message = "Phiếu nhập phải có ít nhất một dòng hàng hóa")
    @Valid
    private List<ImportDetailRequest> details;
}
