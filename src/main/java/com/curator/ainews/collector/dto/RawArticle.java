package com.curator.ainews.collector.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawArticle {
    private String title;
    private String url;
    private String source;
    private String content;
    private LocalDateTime publishedAt;
}
