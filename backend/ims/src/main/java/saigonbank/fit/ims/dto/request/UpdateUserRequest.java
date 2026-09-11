package saigonbank.fit.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "Họ tên là bắt buộc")
    private String fullName;

    private String email;

    private String phone;

    private Boolean isActive;

    /** Danh sách tên role: ADMIN, USER, CONTROLLER */
    private List<String> roles;
}
