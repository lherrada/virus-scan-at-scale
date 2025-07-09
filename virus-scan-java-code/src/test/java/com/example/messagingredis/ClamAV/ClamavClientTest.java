package com.example.messagingredis.ClamAV;

import com.example.messagingredis.ClamAV.Constants.ScanResult;
import com.example.messagingredis.ClamAV.Constants.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ClamavClientTest {

    private ClamavClient client;

    private final String socketPath = "/fake/socket";

    @BeforeEach
    public void setUp() {
        try (
                MockedStatic<Path> pathMock = mockStatic(Path.class);
                MockedStatic<UnixDomainSocketAddress> addressMock = mockStatic(UnixDomainSocketAddress.class);
        ) {
            Path mockPath = mock(Path.class);
            pathMock.when(() -> Path.of(socketPath)).thenReturn(mockPath);

            UnixDomainSocketAddress mockAddress = mock(UnixDomainSocketAddress.class);
            addressMock.when(() -> UnixDomainSocketAddress.of(mockPath)).thenReturn(mockAddress);
        }
        client = new ClamavClient(socketPath);
    }

    @Test
    public void testBuildScanResult_Clean() {
        String response = "stream: OK";
        ScanResultInfo result = client.buildScanResult("hash123", response);
        assertNotNull(result);
        assertEquals("hash123", result.getHashValue());
        assertEquals(ScanResult.CLEAN, result.getScanResult());
        assertEquals(ScanStatus.COMPLETED, result.getScanStatus());
    }

    @Test
    public void testBuildScanResult_Infected() {
        String response = "stream: EICAR-Test-File FOUND";
        ScanResultInfo result = client.buildScanResult("hash456", response);
        assertNotNull(result);
        assertEquals("hash456", result.getHashValue());
        assertEquals(ScanResult.INFECTED, result.getScanResult());
        assertEquals("EICAR-Test-File", result.getVirusName());
        assertEquals(ScanStatus.COMPLETED, result.getScanStatus());
    }

    @Test
    public void testBuildScanResult_Undetermined() {
        String response = "weird result";
        ScanResultInfo result = client.buildScanResult("hash000", response);
        assertNull(result);
    }

    @Test
    public void testPing_WhenSocketChannelOpenThrowsException() {
        try (MockedStatic<SocketChannel> socketMock = mockStatic(SocketChannel.class)){
            socketMock.when(() -> SocketChannel.open(StandardProtocolFamily.UNIX))
                    .thenThrow(new IOException("simulated failure"));
            assertDoesNotThrow(() -> client.ping());
        }
    }

    @Test
    public void testScan_ReturnsCleanResult() throws Exception {
        byte[] dummyContent = "hello world".getBytes();

        try (MockedStatic<SocketChannel> socketMock = mockStatic(SocketChannel.class)) {
            SocketChannel mockSocket = mock(SocketChannel.class);
            socketMock.when(() -> SocketChannel.open(StandardProtocolFamily.UNIX)).thenReturn(mockSocket);

            // Simulate ClamAV response "stream: OK"
            when(mockSocket.read(any(ByteBuffer.class)))
                    .thenAnswer(invocation -> {
                        ByteBuffer buffer = invocation.getArgument(0);
                        buffer.put("stream: OK".getBytes());
                        return "stream: OK".getBytes().length;
                    })
                    .thenReturn(-1);

            ScanResultInfo result = client.scan("abc123", dummyContent);
            assertNotNull(result);
            assertEquals(ScanResult.CLEAN, result.getScanResult());
            assertEquals("abc123", result.getHashValue());
        }
    }

    @Test
    public void testScan_WhenSocketChannelOpenThrowsException() {
        try (MockedStatic<SocketChannel> socketMock = mockStatic(SocketChannel.class)){
            socketMock.when(() -> SocketChannel.open(StandardProtocolFamily.UNIX))
                    .thenThrow(new IOException("simulated failure"));

            ScanResultInfo result = client.scan("hash888", "test".getBytes());
            assertNull(result);
        }


    }

    @Test
    public void testScan_WhenSocketOpenFails_ReturnsNull() {
        try (
                MockedStatic<SocketChannel> socketMock = mockStatic(SocketChannel.class)
        ) {
            socketMock.when(() -> SocketChannel.open(StandardProtocolFamily.UNIX))
                    .thenThrow(new IOException("fail"));
            ScanResultInfo result = client.scan("failing", "data".getBytes());
            assertNull(result);
        }
    }
}
