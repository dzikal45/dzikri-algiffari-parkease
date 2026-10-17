package com.park.parkease.service.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

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
}
