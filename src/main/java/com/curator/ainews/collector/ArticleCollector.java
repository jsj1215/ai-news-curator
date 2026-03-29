package com.curator.ainews.collector;

import com.curator.ainews.collector.dto.RawArticle;
import java.util.List;

public interface ArticleCollector {

    String getSourceName();

    List<RawArticle> collect();
}
