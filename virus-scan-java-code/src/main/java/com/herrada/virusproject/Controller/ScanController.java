package com.herrada.virusproject.Controller;


import com.herrada.virusproject.ClamAV.Constants.ScanResult;
import com.herrada.virusproject.ClamAV.Constants.ScanStatus;
import com.herrada.virusproject.ClamAV.ScanResultInfo;
import com.herrada.virusproject.Keys.CustomKeyGenerator;
import com.herrada.virusproject.Services.ScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ScanController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanController.class);


    @Autowired
    private ScanService scanService;
    @Autowired
    private CustomKeyGenerator customKeyGenerator;

    //curl -X POST -F "file=@/path/to/your/file.bin" http://localhost:8080/api/sync/scan

    @PostMapping("/sync/scan")
    public ResponseEntity<ScanResultInfo> handleSyncFileScan
            (@RequestParam(name = "file")MultipartFile scanFile)
            throws IOException {
        if (scanFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ScanResultInfo());
        }
        String filename = scanFile.getOriginalFilename();
        byte[] fileRawContent = scanFile.getBytes();
        String hashValue = customKeyGenerator.bytesToHex(fileRawContent);
        Optional<ScanResultInfo> resultInfo = Optional.ofNullable(scanService.syncScan(hashValue,
                filename, fileRawContent));

        if (resultInfo.isPresent()) {
            resultInfo.get().setFileName(filename);
            return ResponseEntity.ok().body(resultInfo.get());
        }
        else {
            ScanResultInfo info = new ScanResultInfo();
            info.setHashValue(hashValue);
            info.setFileName(filename);
            info.setScanStatus(ScanStatus.INCOMPLETED);
            info.setScanResult(ScanResult.UNDETERMINED);
            return ResponseEntity.internalServerError().body(info);
        }
    }

    @PostMapping("/async/scan")
    public ResponseEntity<ScanResultInfo> handleAsyncFileScan(
            @RequestParam(name = "file")MultipartFile scanFile) throws IOException {

        if (scanFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ScanResultInfo());
        }
        String filename = scanFile.getOriginalFilename();
        byte[] fileRawContent = scanFile.getBytes();
        String hashValue = customKeyGenerator.bytesToHex(fileRawContent);
        ScanResultInfo resultInfo = scanService.asyncScan(hashValue, filename, fileRawContent);
        return ResponseEntity.ok().body(resultInfo);
    }

    @GetMapping("/getScanResult/{hashValue}")
    public ResponseEntity<ScanResultInfo> getScanResult(@PathVariable
                                                            String hashValue) {
        ScanResultInfo resultInfo = scanService.getScanResult(hashValue);
        return ResponseEntity.ok().body(resultInfo);
    }
}