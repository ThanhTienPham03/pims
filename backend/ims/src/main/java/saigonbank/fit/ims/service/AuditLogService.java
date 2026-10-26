package saigonbank.fit.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import saigonbank.fit.ims.config.websocket.ActivityWebSocketHandler;
import saigonbank.fit.ims.dto.response.AuditLogResponse;
import saigonbank.fit.ims.dto.response.PageResponse;
import saigonbank.fit.ims.entity.AuditLog;
import saigonbank.fit.ims.entity.User;
import saigonbank.fit.ims.repository.AuditLogRepository;
import saigonbank.fit.ims.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Ghi và truy vấn audit log; logAction dùng REQUIRES_NEW để độc lập với transaction gốc
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

        private final AuditLogRepository auditLogRepository;
        private final ActivityWebSocketHandler activityWebSocketHandler;
        private final UserRepository userRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logAction(String username, String action, String entityName,
                        String entityId, String description) {
                try {
                        AuditLog entry = AuditLog.builder()
                                        .username(username != null ? username : "system")
                                        .action(action)
                                        .entityName(entityName)
                                        .entityId(entityId)
                                        .description(description)
                                        .build();
                        entry = auditLogRepository.save(entry);
                        log.info("[AUDIT LOG] User: '{}' | Action: [{}] | Target: [{}:{}] | Details: {}",
                                        entry.getUsername(), entry.getAction(), entry.getEntityName(),
                                        entry.getEntityId(), entry.getDescription());

                        String fullName = null;
                        if (username != null && !username.isBlank()) {
                                fullName = userRepository.findByUsername(username)
                                                .map(User::getFullName)
                                                .orElse(null);
                        }

                        AuditLogResponse response = mapToResponse(entry, fullName);
                        activityWebSocketHandler.broadcast(response);
                } catch (Exception e) {
                        log.error("Failed to write audit log: action={}, entity={}, id={}: {}",
                                        action, entityName, entityId, e.getMessage());
                }
        }

        @Transactional(readOnly = true)
        public PageResponse<AuditLogResponse> getAuditLogs(
                        String username, String action, String entityName,
                        String fromDate, String toDate, Pageable pageable) {

                LocalDateTime from = fromDate != null && !fromDate.isBlank()
                                ? LocalDateTime.parse(fromDate + "T00:00:00")
                                : null;
                LocalDateTime to = toDate != null && !toDate.isBlank()
                                ? LocalDateTime.parse(toDate + "T23:59:59")
                                : null;

                Page<AuditLog> page = auditLogRepository.findWithFilters(
                                username, action, entityName, from, to, pageable);

                List<String> usernames = page.getContent().stream()
                                .map(AuditLog::getUsername)
                                .filter(u -> u != null && !u.isBlank())
                                .distinct()
                                .toList();

                Map<String, String> userFullNameMap = userRepository.findByUsernameIn(usernames).stream()
                                .filter(u -> u.getUsername() != null && u.getFullName() != null)
                                .collect(Collectors.toMap(
                                                User::getUsername,
                                                User::getFullName,
                                                (existing, replacement) -> existing));

                List<AuditLogResponse> content = page.getContent().stream()
                                .map(log -> mapToResponse(log, userFullNameMap.get(log.getUsername())))
                                .toList();

                return PageResponse.<AuditLogResponse>builder()
                                .content(content)
                                .pageNumber(page.getNumber())
                                .pageSize(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .isLast(page.isLast())
                                .build();
        }

        private AuditLogResponse mapToResponse(AuditLog log, String fullName) {
                return AuditLogResponse.builder()
                                .id(log.getId())
                                .username(log.getUsername())
                                .fullName(fullName)
                                .action(log.getAction())
                                .entityName(log.getEntityName())
                                .entityId(log.getEntityId())
                                .description(log.getDescription())
                                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now())
                                .build();
        }
}
