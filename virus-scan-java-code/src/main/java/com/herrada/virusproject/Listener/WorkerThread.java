package com.herrada.virusproject.Listener;

import com.herrada.virusproject.ClamAV.ScanRequest;
import com.herrada.virusproject.ClamAV.ScanResultInfo;
import com.herrada.virusproject.Services.ScanService;
import com.herrada.virusproject.Services.TaskQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class WorkerThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThread.class);
    private final TaskQueueService taskQueueService;
    private final ScanService scanService;

    public WorkerThread(TaskQueueService taskQueueService,
                        ScanService scanService) {
        this.taskQueueService = taskQueueService;
        this.scanService = scanService;
    }

    @Override
    public void run() {
        while (true) {
            try {
                ScanRequest request = taskQueueService.blockingDequeue(0);
                String uniqueId = null;
                String fileName = null;
                byte[] data = new byte[0];
                if (Objects.nonNull(request)) {
                    uniqueId = request.getUniqueId();
                    fileName = request.getFilename();
                    data = request.getData();
                }

                if (data != null && data.length > 0) {
                    Optional<ScanResultInfo> scanResultInfo = Optional.ofNullable(scanService
                            .syncScan(uniqueId, fileName, data));
                    if (scanResultInfo.isEmpty()) {
                        // Retry logic
                        LOGGER.info("Scan failed with hashValue={}. Back to the queue ...", uniqueId);
                        taskQueueService.enqueue(request);
                    } else {
                        LOGGER.info("{} processed with result={}",
                                Thread.currentThread().getName(), scanResultInfo.get());
                    }
                }
            } catch (Exception exception) {
                //exception.printStackTrace();
                LOGGER.error(exception.toString());
                if (!taskQueueService.ping()) {
                    break;
                }
            }
        }
    }
}

