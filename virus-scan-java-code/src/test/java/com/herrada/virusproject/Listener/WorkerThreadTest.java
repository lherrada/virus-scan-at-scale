package com.herrada.virusproject.Listener;

import com.herrada.virusproject.ClamAV.Constants.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import com.herrada.virusproject.ClamAV.ScanRequest;
import com.herrada.virusproject.ClamAV.ScanResultInfo;
import com.herrada.virusproject.Services.ScanService;
import com.herrada.virusproject.Services.TaskQueueService;

class WorkerThreadTest {

    @Mock
    private TaskQueueService taskQueueService;

    @Mock
    private ScanService scanService;

    private WorkerThread workerThread;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        workerThread = new WorkerThread(taskQueueService, scanService);
    }

    @Test
    void testSuccessfulScan() {
        // Prepare test data
        String uniqueId = "test-123";
        String fileName = "test.txt";
        byte[] data = "test data".getBytes();
        ScanRequest request = new ScanRequest(uniqueId, fileName, data);
        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setScanResult(ScanResult.CLEAN);

        // Configure mock behavior
        when(taskQueueService.blockingDequeue(0)).thenReturn(request);
        when(scanService.syncScan(uniqueId, fileName, data)).thenReturn(resultInfo);
        when(taskQueueService.ping()).thenReturn(false); // To break the infinite loop

        // Execute
        workerThread.run();

        // Verify
        verify(scanService).syncScan(uniqueId, fileName, data);
        verify(taskQueueService, never()).enqueue(any(ScanRequest.class));
    }

    @Test
    void testFailedScan() {
        // Prepare test data
        String uniqueId = "test-123";
        String fileName = "test.txt";
        byte[] data = "test data".getBytes();
        ScanRequest request = new ScanRequest(uniqueId, fileName, data);

        // Configure mock behavior
        when(taskQueueService.blockingDequeue(0)).thenReturn(request);
        when(scanService.syncScan(uniqueId, fileName, data)).thenReturn(null);
        when(taskQueueService.ping()).thenReturn(false); // To break the infinite loop

        // Execute
        workerThread.run();

        // Verify
        verify(scanService).syncScan(uniqueId, fileName, data);
        verify(taskQueueService).enqueue(request);
    }

    @Test
    void testNullRequest() {
        // Configure mock behavior
        when(taskQueueService.blockingDequeue(0)).thenReturn(null);
        when(taskQueueService.ping()).thenReturn(false); // To break the infinite loop

        // Execute
        workerThread.run();

        // Verify
        verify(scanService, never()).syncScan(any(), any(), any());
        verify(taskQueueService, never()).enqueue(any(ScanRequest.class));
    }

    @Test
    void testExceptionHandling() {
        // Prepare test data
        ScanRequest request = new ScanRequest("test-123", "test.txt", "test data".getBytes());

        // Configure mock behavior
        when(taskQueueService.blockingDequeue(0)).thenThrow(new RuntimeException("Test exception"));
        when(taskQueueService.ping()).thenReturn(false); // To break the infinite loop

        // Execute
        workerThread.run();

        // Verify
        verify(scanService, never()).syncScan(any(), any(), any());
        verify(taskQueueService).ping();
    }
}

