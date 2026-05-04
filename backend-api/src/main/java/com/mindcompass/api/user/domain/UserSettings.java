// 파일: UserSettings.java
// 역할: 사용자 설정 엔티티
// 호출: UserService -> UserSettings

package com.mindcompass.api.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "app_lock_enabled", nullable = false)
    private Boolean appLockEnabled = false;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @Column(name = "daily_reminder_time")
    private LocalTime dailyReminderTime;

    @Column(name = "response_mode", nullable = false, length = 30)
    private String responseMode = "BALANCED";

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserSettings(User user, Boolean appLockEnabled, Boolean notificationEnabled,
                        LocalTime dailyReminderTime, String responseMode) {
        this.user = user;
        this.appLockEnabled = appLockEnabled != null ? appLockEnabled : false;
        this.notificationEnabled = notificationEnabled != null ? notificationEnabled : true;
        this.dailyReminderTime = dailyReminderTime;
        this.responseMode = responseMode != null ? responseMode : "BALANCED";
    }
}
