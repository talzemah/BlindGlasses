package talzemah.blindglasses;

import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import me.aflak.ezcam.EZCam;
import me.aflak.ezcam.EZCamCallback;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    // The delay is for achieve good focus by the camera.
    private static final int CAPTURE_IMAGE_DELAY = 2000;

    // EZCam is an Android library that simplifies the use of Camera 2 API.
    private EZCam camera;
    private TextureView textureView;
    private Timer cameraTimer;
    private String cameraId;
    private EZCamCallback cameraCallback = new EZCamCallback() {
        @Override
        public void onCameraReady() {
            // Set capture settings.

            // Prevents Flash from being used in the theater
            camera.setCaptureSetting(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

            camera.startPreview();
        }

        @Override
        public void onPicture(Image image) {
            // Invoke when image is ready.

            // Create directory.
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            File appDirectory = new File(externalStorageDirectory.getAbsolutePath() + "/BlindGlasses");

            if (!appDirectory.isDirectory())
                appDirectory.mkdirs();

            // Create file name.
            String fileName = String.format(Locale.ENGLISH, "%d.jpg", System.currentTimeMillis());
            File imageFile = new File(appDirectory, fileName);

            try {
                // Save the image in device.
                EZCam.saveImage(image, imageFile);
                refreshGallery(imageFile);

                Log.d(TAG, "Image Saved to: " + imageFile.getAbsolutePath());

            } catch (IOException e) {

                Log.e(TAG, e.toString());
                e.printStackTrace();

            } finally {

                Intent intent = new Intent();
                intent.putExtra("imagePath", imageFile.getAbsolutePath());
                setResult(imageFile.exists() ? RESULT_OK : RESULT_CANCELED, intent);
                finish();
            }
        }

        @Override
        public void onError(String message) {
            // all errors will be passed through this methods

            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }

        @Override
        public void onCameraDisconnected() {
            // camera disconnected
            Log.e(TAG, "onCameraDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView = (TextureView) findViewById(R.id.textureView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        initialCamera();

        if (camera != null && cameraTimer == null)
            takePictureContinuously();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Cancel the timer.
        if (cameraTimer != null) {
            cameraTimer.cancel();
            cameraTimer = null;
        }

        // Close the camera.
        if (camera != null) {
            if (cameraId != null)
                camera.close();
            camera = null;
        }
    }

    private void takePictureContinuously() {
        cameraTimer = new Timer();

        final TimerTask takePictureTask = new TimerTask() {
            @Override
            public void run() {
                if (camera != null && cameraId != null)

                    camera.takePicture();
            }
        };

        cameraTimer.schedule(takePictureTask, CAPTURE_IMAGE_DELAY);
    }

    private void initialCamera() {

        camera = new EZCam(this);

        // Make sure that rear camera exist.
        cameraId = camera.getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK);
        if (cameraId == null) {
            Toast.makeText(this, getString(R.string.rear_camera_not_found), Toast.LENGTH_LONG).show();
            finish();
        } else {

            // Select which camera to open (in our case rear camera).
            camera.selectCamera(cameraId);
            // Set callback to receive camera event.
            camera.setCameraCallback(cameraCallback);
            // Open the camera to prepare preview.
            camera.open(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG, textureView);
        }
    }

    // Show the capture image in gallery.
    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);

        Log.d(TAG, "Image added to gallery");
    }

} // End Activity.