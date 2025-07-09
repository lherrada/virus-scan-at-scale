package com.herrada.virusproject.Services;


import com.herrada.virusproject.ClamAV.ClamavClient;
import com.herrada.virusproject.ClamAV.Constants.ScanResult;
import com.herrada.virusproject.ClamAV.Constants.ScanStatus;
import com.herrada.virusproject.ClamAV.ScanRequest;
import com.herrada.virusproject.ClamAV.ScanResultInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnableCaching
@TestPropertySource(properties = {
        "spring.cache.type=simple"
})
@ActiveProfiles("test")
public class ScanServiceTest {
    @Autowired
    private ScanService scanService;

    @MockBean
    private ClamavClient clamavClient;

    @MockBean
    private TaskQueueService taskQueueService;

    @MockBean
    private RedisTemplate<String, ScanResultInfo> scanResultRedisTemplate;

    @MockBean
    private ValueOperations<String, ScanResultInfo> valueOperations;


    @Test
    void testSyncScan_usesCacheAfterFirstCall() {
        // Arrange
        String hash = "abc123";
        String filename = "file.exe";
        byte[] content = "test data".getBytes();

        ScanResultInfo expected = new ScanResultInfo();
        expected.setHashValue(hash);
        expected.setFileName(filename);
        expected.setScanResult(ScanResult.CLEAN);
        expected.setScanStatus(ScanStatus.COMPLETED);

        when(clamavClient.scan(eq(hash), eq(content)))
                .thenReturn(expected);

        // First call → should call ClamAV
        ScanResultInfo result1 = scanService.syncScan(hash, filename, content);
        // Second call → should come from the cache
        ScanResultInfo result2 = scanService.syncScan(hash, filename, content);

        // Verify
        assertEquals(expected.getHashValue(), result1.getHashValue());
        assertEquals(result1.getHashValue(), result2.getHashValue());
        verify(clamavClient, times(1)).scan(eq(hash), eq(content));
    }

    @Test
    void testAsyncScan_enqueuesRequestAndReturnsExpectedInfo() {
        // Arrange
        String hashValue = "async123";
        String filename = "myfile.exe";
        byte[] content = "test binary".getBytes();

        // Act
        ScanResultInfo result = scanService.asyncScan(hashValue, filename, content);

        // Assert result
        assertNotNull(result);
        assertEquals(hashValue, result.getHashValue());
        assertEquals(filename, result.getFileName());
        assertEquals("/api/getScanResult/" + hashValue, result.getCallbackPath());
        assertEquals(ScanStatus.IN_QUEUE, result.getScanStatus());

        // Capture and verify enqueued request
        ArgumentCaptor<ScanRequest> captor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(taskQueueService, times(1)).enqueue(captor.capture());


        ScanRequest scanRequest = captor.getValue();
        assertEquals(hashValue, scanRequest.getUniqueId());
        assertEquals(filename, scanRequest.getFilename());
        assertArrayEquals(content, scanRequest.getData());
    }

    @Test
    void testGetScanResult_whenKeyExists_returnsCachedResult() {
        // Arrange
        String hash = "hash-exists";
        ScanResultInfo cached = new ScanResultInfo();
        cached.setHashValue(hash);
        cached.setScanStatus(ScanStatus.COMPLETED);

        when(scanResultRedisTemplate.hasKey(hash)).thenReturn(true);
        when(scanResultRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(hash)).thenReturn(cached);

        // Act
        ScanResultInfo result = scanService.getScanResult(hash);

        // Assert
        assertNotNull(result);
        assertEquals(hash, result.getHashValue());
        assertEquals(ScanStatus.COMPLETED, result.getScanStatus());
        verify(scanResultRedisTemplate, times(1)).hasKey(hash);
        verify(valueOperations, times(1)).get(hash);
    }

    @Test
    void testGetScanResult_whenKeyMissing_returnsKeyExpired() {
        // Arrange
        String hash = "hash-missing";
        when(scanResultRedisTemplate.hasKey(hash)).thenReturn(false);

        // Act
        ScanResultInfo result = scanService.getScanResult(hash);

        // Assert
        assertNotNull(result);
        assertEquals(hash, result.getHashValue());
        assertEquals(ScanStatus.KEY_EXPIRED, result.getScanStatus());
        verify(scanResultRedisTemplate, times(1)).hasKey(hash);
        verify(scanResultRedisTemplate, never()).opsForValue();
    }
}