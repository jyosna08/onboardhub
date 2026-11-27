package com.onboardhub.asset;

import static org.assertj.core.api.Assertions.assertThat;

import com.onboardhub.asset.dto.AssignAssetRequest;
import com.onboardhub.asset.dto.CreateAssetRequest;
import com.onboardhub.asset.entity.Asset;
import com.onboardhub.asset.enums.AssetType;
import com.onboardhub.asset.enums.Role;
import com.onboardhub.asset.exception.AssetConflictException;
import com.onboardhub.asset.repository.AssetAssignmentRepository;
import com.onboardhub.asset.repository.AssetRepository;
import com.onboardhub.asset.security.AuthenticatedUser;
import com.onboardhub.asset.service.AssetService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AssignConcurrencyTest {
    @Autowired private AssetService assetService;
    @Autowired private AssetRepository assets;
    @Autowired private AssetAssignmentRepository assignments;

    @BeforeEach
    void clearDatabase() {
        assignments.deleteAll();
        assets.deleteAll();
    }

    @Test
    void onlyOneConcurrentAssignmentSucceeds() throws Exception {
        var response = assetService.createAsset(new CreateAssetRequest(AssetType.LAPTOP, "Dell", "Latitude",
                "CONCURRENT-1", LocalDate.of(2025, 1, 1)));
        int contenders = 8;
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        try {
            List<Future<Boolean>> attempts = submitAssignments(pool, response.id(), ready, start, contenders);
            ready.await();
            start.countDown();
            long successes = collectSuccesses(attempts);
            Asset finalAsset = assets.findById(response.id()).orElseThrow();
            long openRows = assignments.findByAssetIdOrderByAssignedAtDesc(response.id()).stream()
                    .filter(row -> row.getReturnedAt() == null).count();
            assertThat(successes).isEqualTo(1);
            assertThat(finalAsset.getStatus()).isEqualTo(com.onboardhub.asset.enums.AssetStatus.ASSIGNED);
            assertThat(openRows).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Future<Boolean>> submitAssignments(ExecutorService pool, Long assetId,
            CountDownLatch ready, CountDownLatch start, int count) {
        List<Future<Boolean>> attempts = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            long employeeId = index + 1L;
            attempts.add(pool.submit(() -> assignOnce(assetId, employeeId, ready, start)));
        }
        return attempts;
    }

    private boolean assignOnce(Long assetId, Long employeeId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            assetService.assignAsset(assetId, new AssignAssetRequest(employeeId, "Employee " + employeeId,
                    AssetType.LAPTOP), new AuthenticatedUser(99L, "it@example.com", Role.IT, "Ravi IT"));
            return true;
        } catch (AssetConflictException exception) {
            if (!exception.getMessage().contains("not available")) throw exception;
            return false;
        }
    }

    private long collectSuccesses(List<Future<Boolean>> attempts) throws Exception {
        long successes = 0;
        for (Future<Boolean> attempt : attempts) if (attempt.get()) successes++;
        return successes;
    }
}
