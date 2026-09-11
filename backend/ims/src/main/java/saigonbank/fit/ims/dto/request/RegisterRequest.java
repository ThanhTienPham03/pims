package saigonbank.fit.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {

    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    private String username;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String fullName;

    private String email;

    private String phone;

    /** Danh sách tên role: ADMIN, USER, CONTROLLER */
    private List<String> roles;
}
