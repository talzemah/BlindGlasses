package talzemah.blindglasses;

import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class CameraActivity extends AppCompatActivity {

    private Button takePictureBtn;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        takePictureBtn = (Button) findViewById(R.id.btn_takepicture);
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamera();
            }
        });

    }



    private void startCamera() {
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);



    }

}
