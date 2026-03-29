package com.curator.ainews.evaluator;

/**
 * LLM 호출 추상화 인터페이스.
 * Spring AI 구현체를 기본으로 사용하되, 문제 시 REST 직접 호출 구현체로 교체 가능.
 */
public interface GeminiClient {

    String call(String prompt);
}
