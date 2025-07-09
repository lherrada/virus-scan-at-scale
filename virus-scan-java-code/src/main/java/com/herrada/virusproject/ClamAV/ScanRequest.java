package com.herrada.virusproject.ClamAV;

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
