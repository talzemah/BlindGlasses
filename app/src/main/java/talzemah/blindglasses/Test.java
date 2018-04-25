//package talzemah.blindglasses;
//
///**
// * Created by Tal on 18/04/2018.
// */
//
//
//import android.app.Activity;
//import android.hardware.Camera;
//import android.hardware.Camera.Parameters;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//public class MainActivityy extends Activity {
//
//    Button BUTTONon, BUTTONoff;
//    TextView textview;
//    Camera camera;
//    Parameters parameters;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        textview = (TextView) findViewById(R.id.textView1);
//        BUTTONon = (Button) findViewById(R.id.button1);
//        BUTTONoff = (Button) findViewById(R.id.button2);
//
//        camera = Camera.open();
//        parameters = camera.getParameters();
//
//        BUTTONon.setOnClickListener(new View.OnClickListener() {
//
//            @SuppressWarnings("deprecation")
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//
//                textview.setText("FlashLight ON");
//                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
//                camera.setParameters(parameters);
//                camera.startPreview();
//
//            }
//        });
//
//
//        BUTTONoff.setOnClickListener(new View.OnClickListener() {
//
//            @SuppressWarnings("deprecation")
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//
//                textview.setText("FlashLight OFF");
//                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
//                camera.setParameters(parameters);
//                camera.stopPreview();
//
//            }
//        });
//
//
//    }
//}
