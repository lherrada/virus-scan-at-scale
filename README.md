# Virus-scan-at-scale
Current project implemented in java language, a system that scans files for virus detection.
This system design is meant to handle thousands of files in parallel for virus scan.

We worked on two approaches:
1. Synchronous scan.
2. Asynchronous scan.

## 1. Synchronous scan:
<p align="left">
On this approach, the external user sends the scan request and waits for the result.
It makes use of a caching layer to memorize the result of a scan request. 
In this way, we avoid scanning the same file content twice. Take into account that file
scanning for virus is an expensive operation.  
</p>
We have two different call flows, depending on whether we have a cache hit or miss.
These are the call flows:

![img.png](syn-call-cache-hit.png)
![img.png](syn-call-cache-miss.png)


## 2. Asynchronous scan:
   On this approach, the external user sends the scan request and does not wait for the response.
   Instead, it queries for the result at a later time.
   This is the call flow:

![img_1.png](async-call.png)

