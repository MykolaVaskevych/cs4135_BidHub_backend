package com.bidhub.auction;

import com.bidhub.auction.domain.service.BidValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AuctionServiceApplicationTests {

    // Phase 6 (bid-validation ACL) is blocked on Xunze's GET /api/accounts/{userId}.
    // Stub the interface so the Spring context can load in tests.
    @MockitoBean
    BidValidationService bidValidationService;

    @Test
    void contextLoads() {
    }
}
