package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateLoginResponse {
    /** Email đã che (ví dụ: ng***@gmail.com) */
    private String maskedEmail;
    private String message;
}
