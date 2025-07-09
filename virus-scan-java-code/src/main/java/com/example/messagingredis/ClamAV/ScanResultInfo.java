package com.example.messagingredis.ClamAV;

import com.example.messagingredis.ClamAV.Constants.ScanResult;
import com.example.messagingredis.ClamAV.Constants.ScanStatus;
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
