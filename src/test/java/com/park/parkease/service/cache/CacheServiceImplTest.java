package com.park.parkease.service.cache;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheServiceImplTest {
    @Test
    void set_thenGet_returnsUnexpiredValue() {
        CacheService<String, String> cacheService = new CacheServiceImpl<>();

        cacheService.set("parking-lot", "LOT-JKT01", Duration.ofMinutes(1));

        assertThat(cacheService.get("parking-lot")).contains("LOT-JKT01");
    }

    @Test
    void doubleRequest_withSameActiveKey_marksSecondRequestAsDuplicate() {
        CacheService<String, String> cacheService = new CacheServiceImpl<>();

        boolean firstRequest = cacheService.doubleRequest("reservation-1", Duration.ofMinutes(1));
        boolean secondRequest = cacheService.doubleRequest("reservation-1", Duration.ofMinutes(1));

        assertThat(firstRequest).isFalse();
        assertThat(secondRequest).isTrue();
    }

    @Test
    void lockRequest_returnsOperationResult() {
        CacheService<String, String> cacheService = new CacheServiceImpl<>();

        String result = cacheService.lockRequest("LOT-JKT01", () -> "locked");

        assertThat(result).isEqualTo("locked");
    }

    @Test
    void lockRequest_removesLockWhenOperationFinishes() {
        CacheServiceImpl<String, String> cacheService = new CacheServiceImpl<>();

        cacheService.lockRequest("LOT-JKT01", () -> null);

        assertThat(map(cacheService, "requestLocks")).doesNotContainKey("LOT-JKT01");
    }

    @Test
    void doubleRequest_removesRequestWhenTtlExpires() throws InterruptedException {
        CacheServiceImpl<String, String> cacheService = new CacheServiceImpl<>();

        cacheService.doubleRequest("reservation-1", Duration.ofMillis(20));
        Thread.sleep(100);

        assertThat(map(cacheService, "requestExpirations")).doesNotContainKey("reservation-1");
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> map(CacheServiceImpl<String, String> cacheService, String fieldName) {
        return (Map<String, ?>) ReflectionTestUtils.getField(cacheService, fieldName);
    }
}
