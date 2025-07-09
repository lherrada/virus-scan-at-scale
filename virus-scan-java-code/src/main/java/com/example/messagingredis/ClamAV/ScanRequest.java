package com.example.messagingredis.ClamAV;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ScanRequest {
    String uniqueId;
    String filename;
    byte[] data;
}
