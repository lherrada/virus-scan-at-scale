package com.example.messagingredis.Controller;

import com.example.messagingredis.ClamAV.Constants.ScanResult;
import com.example.messagingredis.ClamAV.Constants.ScanStatus;
import com.example.messagingredis.ClamAV.ScanResultInfo;
import com.example.messagingredis.Keys.CustomKeyGenerator;
import com.example.messagingredis.Services.ScanService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScanService scanService;

    @MockBean
    private CustomKeyGenerator customKeyGenerator;

    @Test
    void testHandleSyncFileScan_success() throws Exception {
        byte[] fileContent = "dummy data".getBytes();
        String hash = "deadbeef";
        String filename = "test.txt";

        MockMultipartFile file = new MockMultipartFile(
                "file", filename, MediaType.TEXT_PLAIN_VALUE, fileContent);

        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setHashValue(hash);
        resultInfo.setScanResult(ScanResult.CLEAN);
        resultInfo.setScanStatus(ScanStatus.COMPLETED);

        Mockito.when(customKeyGenerator.bytesToHex(fileContent)).thenReturn(hash);
        Mockito.when(scanService.syncScan(eq(hash), eq(filename), eq(fileContent)))
                .thenReturn(resultInfo);

        mockMvc.perform(multipart("/api/sync/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hashValue").value(hash))
                .andExpect(jsonPath("$.scanResult").value("CLEAN"))
                .andExpect(jsonPath("$.scanStatus").value("COMPLETED"));
    }

    @Test
    void testHandleSyncFileScan_emptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/sync/scan").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHandleAsyncFileScan_success() throws Exception {
        byte[] fileContent = "async data".getBytes();
        String hash = "beefdead";
        String filename = "async.txt";

        MockMultipartFile file = new MockMultipartFile(
                "file", filename, MediaType.TEXT_PLAIN_VALUE, fileContent);

        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setHashValue(hash);
        resultInfo.setScanResult(ScanResult.CLEAN);
        resultInfo.setScanStatus(ScanStatus.IN_QUEUE);

        Mockito.when(customKeyGenerator.bytesToHex(fileContent)).thenReturn(hash);
        Mockito.when(scanService.asyncScan(eq(hash), eq(filename), eq(fileContent)))
                .thenReturn(resultInfo);

        mockMvc.perform(multipart("/api/async/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanStatus").value("IN_QUEUE"));
    }

    @Test
    void testGetScanResult_success() throws Exception {
        String hash = "hash123";
        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setHashValue(hash);
        resultInfo.setScanStatus(ScanStatus.COMPLETED);
        resultInfo.setScanResult(ScanResult.CLEAN);

        Mockito.when(scanService.getScanResult(eq(hash))).thenReturn(resultInfo);

        mockMvc.perform(get("/api/getScanResult/{hashValue}", hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hashValue").value(hash))
                .andExpect(jsonPath("$.scanResult").value("CLEAN"))
                .andExpect(jsonPath("$.scanStatus").value("COMPLETED"));
    }
}