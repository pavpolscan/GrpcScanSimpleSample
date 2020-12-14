package com.sngsnd.gridscan;

import io.grpc.internal.JsonParser;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class ScanServiceImpl extends ScanServiceGrpc.ScanServiceImplBase {
    @Override
    public void singleScan(SingleScanRequest request, StreamObserver<SingleScanResponse> responseObserver) {
//        super.singleScan(request, responseObserver);

        SingleScan singleScan=request.getSingleScan();
        try {
            System.out.println("["+JsonParser.parse(singleScan.getScanInfo())+"]");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        String result = "Scanned code is:"+singleScan;

        SingleScanResponse response=SingleScanResponse.newBuilder()
                .setResult(result)
                .build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();
    }
}
