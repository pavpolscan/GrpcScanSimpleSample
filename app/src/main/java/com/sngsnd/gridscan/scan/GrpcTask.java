package com.sngsnd.gridscan.scan;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sngsnd.gridscan.ScanServiceGrpc;
import com.sngsnd.gridscan.SingleScan;
import com.sngsnd.gridscan.SingleScanRequest;
import com.sngsnd.gridscan.SingleScanResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

class GrpcTask extends AsyncTask<String, Void, String> {
    private final WeakReference<Activity> activityReference;
    private ManagedChannel channel;

    GrpcTask(Activity activity) {
        this.activityReference = new WeakReference<Activity>(activity);
    }



    @Override
    protected String doInBackground(String... params) {
        String host = params[0];
        String message = params[1];
        String portStr = params[2];
        int port = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);
        try {
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            ScanServiceGrpc.ScanServiceBlockingStub stub = ScanServiceGrpc.newBlockingStub(channel);

            SingleScan singleScan = SingleScan.newBuilder()
                    .setScanInfo(message)
                    .build();

            SingleScanRequest singleScanRequest = SingleScanRequest.newBuilder()
                    .setSingleScan(singleScan)
                    .build();

            SingleScanResponse response = stub.singleScan(singleScanRequest);

//                GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
//                HelloRequest request = HelloRequest.newBuilder().setName(message).build();
//                HelloReply reply = stub.sayHello(request);
            return response.getResult();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            return String.format("Failed... : %n%s", sw);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        System.out.println("terminating gRPC channel...");
        try {
            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            System.out.println("successfully terminated");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Activity activity = activityReference.get();
        if (activity == null) {
            return;
        }
        TextView resultText = (TextView) activity.findViewById(R.id.grpc_response_text);
        Button sendButton = (Button) activity.findViewById(R.id.send_button);
        resultText.setText(result);
        sendButton.setEnabled(true);
    }
}
