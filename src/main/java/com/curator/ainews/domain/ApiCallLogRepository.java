package com.curator.ainews.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {

    long countByApiNameAndCalledAtAfter(String apiName, LocalDateTime after);

    void deleteByCalledAtBefore(LocalDateTime before);
}
