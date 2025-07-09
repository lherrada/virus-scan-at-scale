package com.herrada.virusproject.ClamAV;
import com.herrada.virusproject.ClamAV.Constants.ScanResult;
import com.herrada.virusproject.ClamAV.Constants.ScanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import java.net.UnixDomainSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClamavClient {
    SocketChannel socketChannel;
    String socketPathStr;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClamavClient.class);
    private static final int CHUNK_SIZE = 2048;

    public ClamavClient(String socketPathStr) {
        this.socketPathStr = socketPathStr;
        LOGGER.info("Socket path: {}", socketPathStr);
    }

    public void openConnection(String socketPathStr) throws IOException {
        assert socketPathStr != null;
        Path socketPath = Path.of(socketPathStr);
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
        socketChannel.connect(address);
    }

    ScanResultInfo buildScanResult(String hashValue, String scanResponse) {
        //stream: Win.Test.EICAR_HDB-1 FOUND
        //stream: OK
        String pattern1 = "stream: (.*) FOUND";
        String pattern2 = "stream: OK";
        Pattern r1 = Pattern.compile(pattern1);
        Pattern r2 = Pattern.compile(pattern2);
        Matcher m1 = r1.matcher(scanResponse);
        Matcher m2 = r2.matcher(scanResponse);
        ScanResultInfo resultInfo = new ScanResultInfo();
        resultInfo.setHashValue(hashValue);
        if (m1.find()) {
            String virusName = m1.group(1);
            resultInfo.setScanResult(ScanResult.INFECTED);
            resultInfo.setVirusName(virusName);
            resultInfo.setScanStatus(ScanStatus.COMPLETED);
        } else if (m2.find()) {
          resultInfo.setScanResult(ScanResult.CLEAN);
            resultInfo.setScanStatus(ScanStatus.COMPLETED);
        } else {
            return null;
        }
        return resultInfo;
    }

    public ScanResultInfo scan(String hashValue, byte[] contentToScan) {
        try {
            this.openConnection(socketPathStr);
            // Send INSTREAM command
            socketChannel.write(ByteBuffer.wrap("zINSTREAM\0".getBytes()));
            // Send file contents in chunks
            InputStream inputStream = new ByteArrayInputStream(contentToScan);
            ReadableByteChannel channel = Channels.newChannel(inputStream);

            ByteBuffer chunkSizeBuffer = ByteBuffer.allocate(4);
            ByteBuffer chunkBuffer = ByteBuffer.allocate(CHUNK_SIZE);
            int bytesRead;

            while ((bytesRead = channel.read(chunkBuffer)) > 0) {
                   chunkBuffer.flip();
                   chunkSizeBuffer.clear();
                   chunkSizeBuffer.putInt(bytesRead);
                   chunkSizeBuffer.flip();
                   socketChannel.write(chunkSizeBuffer);
                   socketChannel.write(chunkBuffer);
                   chunkBuffer.clear();
            }
            // Send terminating zero-length chunk
            chunkSizeBuffer.clear();
            chunkSizeBuffer.putInt(0);
            chunkSizeBuffer.flip();
            socketChannel.write(chunkSizeBuffer);
            // Read response
            return buildScanResult(hashValue, readResponseFromSocket());
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    private String readResponseFromSocket() throws IOException {
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        ByteBuffer responseBuffer = ByteBuffer.allocate(4096);
        int bytes;
        int totalBytes = 0;
        while ((bytes = socketChannel.read(responseBuffer)) > 0) {
            totalBytes +=bytes;
            responseBuffer.flip();
            byte[] data = new byte[bytes];
            responseBuffer.get(data);
            responseStream.write(data);
            responseBuffer.clear();
        }

        String response = responseStream.toString().trim();
        LOGGER.info("Bytes read from Unix socket: {}, Message Received from ClamAV -> {}", totalBytes, response);
        return response;
    }

    //Send "PING" command to Clamav through a Unix socket
    public void ping() {
        try {
            final String message = "PING";
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            openConnection(socketPathStr);
            socketChannel.write(buffer);
            readResponseFromSocket();
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
