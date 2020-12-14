package com.sngsnd.gridscan.scan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.sngsnd.gridscan.ScanServiceImpl;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ControllerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        System.out.println("Start gRPC server");

        try {
            Server server= ServerBuilder
                .forPort(50051)
                .addService(new ScanServiceImpl())
                .build();

            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Received shutdown request");
                server.shutdown();
                System.out.println("Successfully stopped the server");
            } ));

            server.awaitTermination();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}