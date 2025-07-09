package com.herrada.virusproject.Listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import com.herrada.virusproject.ClamAV.ScanRequest;
import com.herrada.virusproject.ClamAV.ScanResultInfo;
import com.herrada.virusproject.Services.ScanService;
import com.herrada.virusproject.Services.TaskQueueService;

class WorkerThreadTest {

    private TaskQueueService taskQueueService;
    private ScanService scanService;

    @BeforeEach
    void setUp() {
        taskQueueService = mock(TaskQueueService.class);
        scanService = mock(ScanService.class);
    }

    @Test
    void run_shouldProcessOneRequestSuccessfully() throws Exception {
        // Given
        byte[] data = "test".getBytes();
        ScanRequest request = new ScanRequest("abc123", "file.exe", data);
        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setHashValue("abc123");

        when(taskQueueService.blockingDequeue(0))
                .thenReturn(request)
                .thenThrow(new RuntimeException("stop loop"));
        when(scanService.syncScan(eq("abc123"), eq("file.exe"), eq(data)))
                .thenReturn(resultInfo);
        when(taskQueueService.ping()).thenReturn(false); // break on exception

        // When
        WorkerThread worker = new WorkerThread(taskQueueService, scanService);
        Thread thread = new Thread(worker);
        thread.start();
        thread.join();  // wait for thread to stop

        // Then
        verify(scanService, times(1)).syncScan(eq("abc123"), eq("file.exe"), eq(data));
        verify(taskQueueService, never()).enqueue(any());
    }

    @Test
    void run_shouldRetryWhenScanResultIsNull() throws Exception {
        // Given
        byte[] data = "retry".getBytes();
        ScanRequest request = new ScanRequest("retry123", "retry.exe", data);

        when(taskQueueService.blockingDequeue(0))
                .thenReturn(request)
                .thenThrow(new RuntimeException("stop loop"));
        when(scanService.syncScan(eq("retry123"), eq("retry.exe"), eq(data)))
                .thenReturn(null);
        when(taskQueueService.ping()).thenReturn(false);

        // When
        WorkerThread worker = new WorkerThread(taskQueueService, scanService);
        Thread thread = new Thread(worker);
        thread.start();
        thread.join();

        // Then
        verify(taskQueueService, times(1)).enqueue(eq(request));
    }

    @Test
    void run_shouldExitWhenPingReturnsFalseAfterException() throws Exception {
        // Given
        when(taskQueueService.blockingDequeue(0)).thenThrow(new RuntimeException("Test exception"));
        when(taskQueueService.ping()).thenReturn(false);

        // When
        WorkerThread worker = new WorkerThread(taskQueueService, scanService);
        Thread thread = new Thread(worker);
        thread.start();
        thread.join();

        // Then
        verify(taskQueueService, times(1)).ping();
    }
}

