package com.example.messagingredis.Services;

import com.example.messagingredis.ClamAV.ClamavClient;
import com.example.messagingredis.ClamAV.ScanRequest;
import com.example.messagingredis.ClamAV.Constants.ScanStatus;
import com.example.messagingredis.ClamAV.ScanResultInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanService.class);

    @Autowired
    ClamavClient clamavClient;

    @Autowired
    private TaskQueueService taskQueueService;

    @Autowired
    private RedisTemplate<String, ScanResultInfo> scanResultRedisTemplate;


    //@Cacheable(value = "scanCache", keyGenerator = "customKeyGenerator", unless="#result == null")
    @Cacheable(value = "scanCache", key="#hashValue", unless="#result == null")
    public ScanResultInfo syncScan(String hashValue, String filename, byte[] contentToScan) {
        int sizeBytes = contentToScan.length;
        LOGGER.info("Sync call with cache miss. Filename=[{}],size=[{}] bytes",
                filename, sizeBytes);
        return clamavClient.scan(hashValue, contentToScan);
    }

    public ScanResultInfo asyncScan(String hashValue, String filename, byte[] contentToScan) {
        ScanRequest scanRequest = new ScanRequest(hashValue, filename, contentToScan);
        String callbackPath = "/api/getScanResult/" + hashValue;
        taskQueueService.enqueue(scanRequest);
        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setScanStatus(ScanStatus.IN_QUEUE);
        resultInfo.setHashValue(hashValue);
        resultInfo.setCallbackPath(callbackPath);
        resultInfo.setFileName(filename);
        return resultInfo;
    }

    public ScanResultInfo getScanResult(String hashValue) {
        if (scanResultRedisTemplate.hasKey(hashValue)) {
            return scanResultRedisTemplate.opsForValue().get(hashValue);
        }
        else {
            ScanResultInfo resultInfo = new ScanResultInfo();
            resultInfo.setHashValue(hashValue);
            resultInfo.setScanStatus(ScanStatus.KEY_EXPIRED);
            return resultInfo;
        }
    }
}
