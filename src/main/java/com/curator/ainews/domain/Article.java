package com.curator.ainews.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_article_url", columnList = "url", unique = true),
    @Index(name = "idx_article_sent_at", columnList = "sentAt"),
    @Index(name = "idx_article_collected_at", columnList = "collectedAt"),
    @Index(name = "idx_article_total_score", columnList = "totalScore")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    @Column(nullable = false)
    private String source;

    @Column(columnDefinition = "CLOB")
    private String content;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    private Double practicalityScore;
    private Double freshnessScore;
    private Double depthScore;
    private Double totalScore;

    @Column(columnDefinition = "CLOB")
    private String summary;

    @Builder.Default
    private Boolean sent = false;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}
