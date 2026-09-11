package saigonbank.fit.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyClosingResponse {
    private Long id;
    private Integer year;
    private Integer month;
    private Boolean isLocked;
    private LocalDateTime lockedAt;
    private String lockedBy;
    private LocalDateTime unlockedAt;
    private String unlockedBy;
}
