package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@saigonbank.vn}")
    private String fromAddress;

    @Async
    public void sendOtpEmail(String toEmail, String username, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "Saigonbank PIMS");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đăng nhập PIMS — " + otpCode);
            helper.setText(buildOtpEmailBody(username, otpCode), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to {} for user '{}'", toEmail, username);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {} for user '{}': {}", toEmail, username, e.getMessage(), e);
        }
    }

    private String buildOtpEmailBody(String username, String otpCode) {
        String template = """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #F0F4F8; padding: 40px 0; margin: 0;">
                  <div style="max-width: 520px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.08);">
                    <div style="background: linear-gradient(135deg, #002B54 0%, #0056A3 100%); padding: 32px 40px; text-align: center;">
                      <div style="font-size: 22px; font-weight: 800; color: #FFFFFF; letter-spacing: -0.02em;">Saigonbank PIMS</div>
                      <div style="font-size: 13px; color: rgba(255,255,255,0.65); margin-top: 4px;">Hệ thống Quản lý Ấn phẩm & Văn phòng phẩm</div>
                    </div>
                    <div style="padding: 40px;">
                      <p style="color: #374151; font-size: 15px; margin: 0 0 8px;">Xin chào <strong>{{username}}</strong>,</p>
                      <p style="color: #6B7280; font-size: 14px; margin: 0 0 28px;">Mã xác thực đăng nhập của bạn là:</p>
                      <div style="background: #F0F7FF; border: 2px dashed #0056A3; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 28px;">
                        <div style="font-size: 40px; font-weight: 800; letter-spacing: 12px; color: #0056A3; font-family: 'Courier New', monospace;">{{otpCode}}</div>
                        <div style="font-size: 13px; color: #6B7280; margin-top: 8px;">Mã có hiệu lực trong <strong>5 phút</strong></div>
                      </div>
                      <p style="color: #9CA3AF; font-size: 13px; margin: 0;">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này.</p>
                    </div>
                    <div style="background: #F9FAFB; padding: 16px 40px; text-align: center; border-top: 1px solid #E5E7EB;">
                      <p style="color: #9CA3AF; font-size: 12px; margin: 0;">© 2026 Ngân hàng TMCP Sài Gòn Công Thương</p>
                    </div>
                  </div>
                </body>
                </html>
                """;
        return template.replace("{{username}}", username != null ? username : "")
                       .replace("{{otpCode}}", otpCode != null ? otpCode : "");
    }
}
