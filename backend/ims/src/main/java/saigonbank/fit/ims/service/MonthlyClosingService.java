package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.dto.response.MonthlyClosingResponse;
import saigonbank.fit.ims.entity.MonthlyClosing;
import saigonbank.fit.ims.repository.MonthlyClosingRepository;

import java.time.LocalDateTime;
import java.util.List;

// Quản lý khóa/mở khóa kỳ kế toán (chỉ CONTROLLER thực hiện)
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyClosingService {

    private final MonthlyClosingRepository monthlyClosingRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public MonthlyClosingResponse lockMonth(Integer year, Integer month, String username) {
        MonthlyClosing closing = monthlyClosingRepository.findByYearAndMonth(year, month)
                .orElseGet(() -> MonthlyClosing.builder()
                        .year(year)
                        .month(month)
                        .build());

        closing.setIsLocked(true);
        closing.setLockedAt(LocalDateTime.now());
        closing.setLockedBy(username);

        closing = monthlyClosingRepository.save(closing);
        log.info("Locked month {}/{} by {}", month, year, username);
        auditLogService.logAction(username, "LOCK_MONTH", "MONTHLY_CLOSING",
                year + "/" + String.format("%02d", month),
                "Khóa sổ kỳ kế toán Tháng " + month + "/" + year);
        return mapToResponse(closing);
    }

    @Transactional
    public MonthlyClosingResponse unlockMonth(Integer year, Integer month, String username) {
        MonthlyClosing closing = monthlyClosingRepository.findByYearAndMonth(year, month)
                .orElseGet(() -> MonthlyClosing.builder()
                        .year(year)
                        .month(month)
                        .build());

        closing.setIsLocked(false);
        closing.setUnlockedAt(LocalDateTime.now());
        closing.setUnlockedBy(username);

        closing = monthlyClosingRepository.save(closing);
        log.info("Unlocked month {}/{} by {}", month, year, username);
        auditLogService.logAction(username, "UNLOCK_MONTH", "MONTHLY_CLOSING",
                year + "/" + String.format("%02d", month),
                "Mở khóa kỳ kế toán Tháng " + month + "/" + year);
        return mapToResponse(closing);
    }

    @Transactional(readOnly = true)
    public List<MonthlyClosingResponse> getAllClosings() {
        return monthlyClosingRepository.findAllByOrderByYearDescMonthDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isLocked(Integer year, Integer month) {
        return monthlyClosingRepository.isLocked(year, month);
    }

    private MonthlyClosingResponse mapToResponse(MonthlyClosing m) {
        return MonthlyClosingResponse.builder()
                .id(m.getId())
                .year(m.getYear())
                .month(m.getMonth())
                .isLocked(m.getIsLocked())
                .lockedAt(m.getLockedAt())
                .lockedBy(m.getLockedBy())
                .unlockedAt(m.getUnlockedAt())
                .unlockedBy(m.getUnlockedBy())
                .build();
    }
}
