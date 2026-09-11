package saigonbank.fit.ims.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

// In-memory OTP store với TTL 5 phút (single-instance)
@Slf4j
@Component
public class OtpStore {

    public static final int OTP_TTL_MINUTES = 5;

    private record OtpEntry(String code, LocalDateTime expiredAt) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();

    public void save(String username, String otpCode) {
        store.put(username, new OtpEntry(otpCode, LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES)));
        log.debug("OTP saved for user '{}', expires at {}", username, store.get(username).expiredAt());
    }

    public boolean verify(String username, String otpCode) {
        OtpEntry entry = store.get(username);
        if (entry == null) {
            log.warn("No OTP found for user '{}'", username);
            return false;
        }
        if (LocalDateTime.now().isAfter(entry.expiredAt())) {
            store.remove(username);
            log.warn("OTP expired for user '{}'", username);
            return false;
        }
        if (!entry.code().equals(otpCode)) {
            log.warn("Invalid OTP for user '{}'", username);
            return false;
        }
        store.remove(username);
        return true;
    }

    public boolean hasPending(String username) {
        OtpEntry entry = store.get(username);
        return entry != null && LocalDateTime.now().isBefore(entry.expiredAt());
    }

    public void remove(String username) {
        store.remove(username);
    }
}
