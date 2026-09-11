package saigonbank.fit.ims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log for all important business operations.
 * Records: CREATE, UPDATE, DELETE, CANCEL, LOCK_MONTH, UNLOCK_MONTH, LOGIN
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static final class Action {
        public static final String CREATE       = "CREATE";
        public static final String UPDATE       = "UPDATE";
        public static final String DELETE       = "DELETE";
        public static final String CANCEL       = "CANCEL";
        public static final String LOCK_MONTH   = "LOCK_MONTH";
        public static final String UNLOCK_MONTH = "UNLOCK_MONTH";
        public static final String LOGIN        = "LOGIN";
        public static final String DEACTIVATE   = "DEACTIVATE";
        private Action() {}
    }
}
