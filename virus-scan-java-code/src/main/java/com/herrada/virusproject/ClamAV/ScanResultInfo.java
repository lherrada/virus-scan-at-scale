package com.herrada.virusproject.ClamAV;

import com.herrada.virusproject.ClamAV.Constants.ScanResult;
import com.herrada.virusproject.ClamAV.Constants.ScanStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class ScanResultInfo {
    String virusName;
    ScanResult scanResult;
    String hashValue;
    String callbackPath;
    ScanStatus scanStatus;
    String fileName;
}
