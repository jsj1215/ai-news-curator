package com.curator.ainews.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_call_logs", indexes = {
    @Index(name = "idx_api_call_called_at", columnList = "calledAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String apiName;

    @Column(nullable = false)
    private LocalDateTime calledAt;

    @PrePersist
    protected void onCreate() {
        if (calledAt == null) {
            calledAt = LocalDateTime.now();
        }
    }
}
