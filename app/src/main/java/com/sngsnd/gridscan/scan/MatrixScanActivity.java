/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sngsnd.gridscan.scan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.scandit.datacapture.barcode.data.Symbology;
import com.scandit.datacapture.barcode.tracking.capture.BarcodeTracking;
import com.scandit.datacapture.barcode.tracking.capture.BarcodeTrackingListener;
import com.scandit.datacapture.barcode.tracking.capture.BarcodeTrackingSession;
import com.scandit.datacapture.barcode.tracking.capture.BarcodeTrackingSettings;
import com.scandit.datacapture.barcode.tracking.data.TrackedBarcode;
import com.scandit.datacapture.barcode.tracking.ui.overlay.BarcodeTrackingBasicOverlay;
import com.scandit.datacapture.core.capture.DataCaptureContext;
import com.scandit.datacapture.core.data.FrameData;
import com.scandit.datacapture.core.source.Camera;
import com.scandit.datacapture.core.source.CameraSettings;
import com.scandit.datacapture.core.source.FrameSourceState;
import com.scandit.datacapture.core.source.VideoResolution;
import com.scandit.datacapture.core.ui.DataCaptureView;
import com.sngsnd.gridscan.scan.ScanResult;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

public class MatrixScanActivity extends CameraPermissionActivity
        implements BarcodeTrackingListener {

    // Enter your Scandit License key here.
    // Your Scandit License key is available via your Scandit SDK web account.
    public static final String SCANDIT_LICENSE_KEY = "ASv/vhGWDtl3OtfyZTe4rLJCQh+CPiIPwDF9exo0XvpUSv5zyHXC6+VxnK2cby7VFghZV/wH+geuRRzqcUjS/c1aj0OrS1zjxzz4GmlUEPBeDgWJoUwb4Gor1+tkR+aPaDZxmvE9QWdrKrs1XBzMuFDW2ZsAiTvdanir6dLMLuwNGlRMw6AbaE/Y9NmBgi1H6BVMX08fDtqDSERc7Jx+ci/5ujT6sb2t6s5+EOVS1SAyttgNB0Gq4Y5wGhJBHewu/+zZ1JKR+lxDfM0F1Bqg6cW+r+2OzVuQlA0okalulyMwraHhNpeuUXwvIxJm1hzr2sp4rVUNdDP4ERUQXvvKY5GOjytgStqRS2rN0xsHo6ae0momKYHvTmZLrjSvYtDeyeQyBiNVNG1yB5bg2jPrqa22EINhZ9vDezxpGegigB4ivIJBqEcr1xoLOxZvZiEWIeLF3zlAlWWwomgrHKI+FqgrEEorsCVly/NfYaFuqg/Hk1DCUSuhyGNR4GprBoJABeGzQd9BPU4v1AOw4sRR5ZoQy/bteqVP8i0tpqX25F0wyztiTOv2nItD2wb4Zp3eagc98KOtIWO+/NZdqFW4y3DzS0geUT4D5eD5SI4+WzT4nHYtSMuN6lynIKv8rgak4JxmB+zAhVJjn/JcGssq9ruQAYjT0WDnCW26z4ngFHjK2WcRR54sP5kgdeulwx3AbznPX2hBwJF8TOO2MPZYGucHM4jWj+wmgLHAhCFQE8njm3+HOlxEhzrKbeimoLsBeBx4qbxyBY8IDzimlHg9jzTCzbZ6mpSzLqBAz6aaWFxiVyUIjmf1oyZqx2kdEifS";

    public static final int REQUEST_CODE_SCAN_RESULTS = 1;

    private Camera camera;
    private BarcodeTracking barcodeTracking;
    private DataCaptureContext dataCaptureContext;

    private String host, port;

    private final HashSet<ScanResult> scanResults = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matrix_scan);

        Intent intent = getIntent();
        host = intent.getStringExtra("host");
        port = intent.getStringExtra("port");

        // Initialize and start the barcode recognition.
        initialize();

        Button doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (scanResults) {
                    // Show new screen displaying a list of all barcodes that have been scanned.
                    Intent intent = ResultsActivity.getIntent(MatrixScanActivity.this, scanResults);
                    intent.putExtra("host", host);
                    intent.putExtra("port", port);
                    startActivityForResult(intent, REQUEST_CODE_SCAN_RESULTS);

                }
            }
        });
    }

    private void initialize() {
        // Create data capture context using your license key.
        dataCaptureContext = DataCaptureContext.forLicenseKey(SCANDIT_LICENSE_KEY);

        // Use the recommended camera settings for the BarcodeTracking mode.
        CameraSettings cameraSettings = BarcodeTracking.createRecommendedCameraSettings();
        // Adjust camera settings - set Full HD resolution.
        cameraSettings.setPreferredResolution(VideoResolution.FULL_HD);
        // Use the default camera and set it as the frame source of the context.
        // The camera is off by default and must be turned on to start streaming frames to the data
        // capture context for recognition.
        // See resumeFrameSource and pauseFrameSource below.
        camera = Camera.getDefaultCamera(cameraSettings);
        if (camera != null) {
            dataCaptureContext.setFrameSource(camera);
        } else {
            throw new IllegalStateException("Sample depends on a camera, which failed to initialize.");
        }

        // The barcode tracking process is configured through barcode tracking settings
        // which are then applied to the barcode tracking instance that manages barcode tracking.
        BarcodeTrackingSettings barcodeTrackingSettings = new BarcodeTrackingSettings();

        // The settings instance initially has all types of barcodes (symbologies) disabled.
        // For the purpose of this sample we enable a very generous set of symbologies.
        // In your own app ensure that you only enable the symbologies that your app requires
        // as every additional enabled symbology has an impact on processing times.
        HashSet<Symbology> symbologies = new HashSet<>();
        symbologies.add(Symbology.EAN13_UPCA);
        symbologies.add(Symbology.EAN8);
        symbologies.add(Symbology.UPCE);
        symbologies.add(Symbology.CODE39);
        symbologies.add(Symbology.CODE128);

        barcodeTrackingSettings.enableSymbologies(symbologies);

        // Create barcode tracking and attach to context.
        barcodeTracking =
                BarcodeTracking.forDataCaptureContext(dataCaptureContext, barcodeTrackingSettings);

        // Register self as a listener to get informed of tracked barcodes.
        barcodeTracking.addListener(this);

        // To visualize the on-going barcode tracking process on screen, setup a data capture view
        // that renders the camera preview. The view must be connected to the data capture context.
        DataCaptureView dataCaptureView = DataCaptureView.newInstance(this, dataCaptureContext);

        // Add a barcode tracking overlay to the data capture view to render the tracked barcodes on
        // top of the video preview. This is optional, but recommended for better visual feedback.
        BarcodeTrackingBasicOverlay.newInstance(barcodeTracking, dataCaptureView);

        // Add the DataCaptureView to the container.
        FrameLayout container = findViewById(R.id.data_capture_view_container);
        container.addView(dataCaptureView);
    }

    @Override
    protected void onPause() {
        pauseFrameSource();
        super.onPause();
    }

    private void pauseFrameSource() {
        // Switch camera off to stop streaming frames.
        // The camera is stopped asynchronously and will take some time to completely turn off.
        // Until it is completely stopped, it is still possible to receive further results, hence
        // it's a good idea to first disable barcode tracking as well.
        barcodeTracking.setEnabled(false);
        camera.switchToDesiredState(FrameSourceState.OFF, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check for camera permission and request it, if it hasn't yet been granted.
        // Once we have the permission the onCameraPermissionGranted() method will be called.
        requestCameraPermission();
    }

    @Override
    public void onCameraPermissionGranted() {
        resumeFrameSource();
    }

    private void resumeFrameSource() {
        // Switch camera on to start streaming frames.
        // The camera is started asynchronously and will take some time to completely turn on.
        barcodeTracking.setEnabled(true);
        camera.switchToDesiredState(FrameSourceState.ON, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCAN_RESULTS
                && resultCode == ResultsActivity.RESULT_CODE_CLEAN) {
            synchronized (scanResults) {
                scanResults.clear();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onObservationStarted(@NonNull BarcodeTracking barcodeTracking) {
        // Nothing to do.
    }

    @Override
    public void onObservationStopped(@NonNull BarcodeTracking barcodeTracking) {
        // Nothing to do.
    }

    // This function is called whenever objects are updated and it's the right place to react to
    // the tracking results.
    @Override
    public void onSessionUpdated(
            @NonNull BarcodeTracking mode,
            @NonNull BarcodeTrackingSession session,
            @NonNull FrameData data
    ) {
        synchronized (scanResults) {

//            Map<Integer, TrackedBarcode> trackedBarcodes=session.getTrackedBarcodes();
//            for (Map.Entry<Integer, TrackedBarcode> barcodeEntry :  trackedBarcodes.entrySet()){
//                System.out.println("about to send grpc data to "+host+":"+port);
//                new GrpcTask(null)
//                        .execute(
//                                host,
//                                barcodeEntry.getValue().toJson(),
//                                port);
//            }


            for (TrackedBarcode trackedBarcode : session.getAddedTrackedBarcodes()) {
                scanResults.add(new ScanResult(trackedBarcode.getBarcode()));
                System.out.println("about to send grpc data to "+host+":"+port);
                new GrpcTask(null)
                        .execute(
                                host,
                                trackedBarcode.getBarcode().getData(),
                                port);
//                Bitmap bmp=data.getImageBuffer().toBitmap();
//                saveFile(this,bmp,"test.png");
//                try (FileOutputStream out = new FileOutputStream("test.png")) {
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 50, out);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }

//            for (Integer removedBarcodeId : session.getRemovedTrackedBarcodes()) {
//                System.out.println("removedBarcodeId "+removedBarcodeId.intValue());
//                TrackedBarcode trackedBarcode=session.getTrackedBarcodes().get(removedBarcodeId);
//                scanResults.add(new ScanResult(trackedBarcode.getBarcode()));
//                System.out.println("about to send grpc data to "+host+":"+port);
//                new GrpcTask(null)
//                        .execute(
//                                host,
//                                "removed: "+trackedBarcode.toJson(),
//                                port);
//            }
        }
    }

    @Override
    protected void onDestroy() {
        dataCaptureContext.removeMode(barcodeTracking);
        super.onDestroy();
    }

    public static void saveFile(Context context, Bitmap b, String picName){

        try {
            FileOutputStream fos = context.openFileOutput(picName, Context.MODE_PRIVATE);
            b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        }
        catch (FileNotFoundException e) {
            Log.d(MatrixScanActivity.class.getName(), "file not found");
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.d(MatrixScanActivity.class.getName(), "io exception");
            e.printStackTrace();
        }
    }
}
