package talzemah.blindglasses;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.http.ServiceCallback;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifierResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY = 2;

    private VisualRecognition visualRecognition;
    private TextToSpeech tts;
    private ResultsFilter filter;

    private Button captureImageBtn;
    private Button selectImageBtn;
    private ImageView resultImageView;
    private ListView resListView;
    private ProgressBar progressBar;

    private File currentPhotoFile;
    private CustomArrayAdapter customAdapter;
    private ArrayList<Result> resArr;
    private ArrayList<Result> filterResArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // todo fix that.
        //turnOffFlash();

        // Create VisualRecognition Object.
        visualRecognition = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
        visualRecognition.setApiKey(getString(R.string.VisualRecognitionApiKey));

        // Create TextToSpeech Object (Android).
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setSpeechRate(0.7f);
                    tts.setPitch(0.9f);
                }
            }
        });

        // Create results filter object.
        filter = new ResultsFilter();

        // ImageView to show the current photo.
        resultImageView = (ImageView) findViewById(R.id.imageView_result);

        // Show the results.
        resListView = (ListView) findViewById(R.id.ListView_results);

        // Progress Bar.
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // Initialize the ArrayList.
        resArr = new ArrayList<>();
        filterResArr = new ArrayList<>();

        // Camera button.
        captureImageBtn = (Button) findViewById(R.id.Btn_CaptureImage);
        captureImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                takePictureFromCamera(REQUEST_IMAGE_CAPTURE);
                resListView.setAdapter(null);
            }
        });

        // Gallery Button.
        selectImageBtn = (Button) findViewById(R.id.Btn_SelectImage);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                takePictureFromCamera(REQUEST_GALLERY);
                resListView.setAdapter(null);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // todo stop speak.
        tts.shutdown();
        tts.stop();
    }

    // speak the final results after sort and filtering.
    private void startSpeak() {

        for (int i = 0; i < filterResArr.size(); i++) {

            tts.speak(filterResArr.get(i).getname(), TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    // Open camera in order to take a picture.
    private void takePictureFromCamera(int request) {

        Intent takePictureIntent;

        if (request == REQUEST_IMAGE_CAPTURE) {
            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        } else {
            takePictureIntent = new Intent(Intent.ACTION_PICK);
            takePictureIntent.setType("image/*");
        }

        // Ensure that there's a camera activity to handle this intent.
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go.
            currentPhotoFile = null;
            try {
                currentPhotoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error occurred while creating the File!", Toast.LENGTH_SHORT).show();
            }

            // Continue only if the File was successfully created
            if (currentPhotoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        currentPhotoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, request);
            }
        }
    }

    // Create image file to populate the captured image.
    private File createImageFile() throws IOException {

        // Create an image file name.
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File file = File.createTempFile(
                imageFileName,   // prefix
                ".jpg",    // suffix
                storageDir       // directory
        );

        return file;
    }

    // Invoke When user come back from camera to the application.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Make sure user return from the camera and there is no some problem.
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            // Show the captured image in resultImageView.
            //showCapturedImage();
            Picasso.with(this)
                    .load(currentPhotoFile)
                    .into(resultImageView);

            usingVisualRecognition();

            // todo same conditions.
        } else if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {

            // Show the captured image in resultImageView.
            Picasso.with(this)
                    .load(data.getData())
                    .into(resultImageView);

            usingVisualRecognition();
        }
    }

    private void compressTheImage() {


    }

    // Analyzes what is in the picture.
    private void usingVisualRecognition() {

        // todo compress the image file.
        compressTheImage();

        InputStream imagesStream = null;

        try {
            imagesStream = new FileInputStream(currentPhotoFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                .imagesFile(imagesStream)
                .imagesFilename(currentPhotoFile.getName())
                // todo  parameters? 
                // .parameters("{\"classifier_ids\": [\"fruits_1462128776\", + \"SatelliteModel_6242312846\"]}")
                .build();

        // Asynchronous Request
        visualRecognition.classify(classifyOptions).enqueue(new ServiceCallback<ClassifiedImages>() {
            @Override
            public void onResponse(ClassifiedImages response) {

                // Here we are in another thread that perform the VR asynchronous request.
                // Every Change on GUI must made by UI thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });

                UpdateAndSortResults(response);
            }

            @Override
            public void onFailure(Exception e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Error while processing the image!", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e(e.getClass().getName(), e.getStackTrace().toString());
            }
        });

        // todo write first of all.
        progressBar.setVisibility(View.VISIBLE);

    }


    // Get the result object from VR, sort the element and update the resArr.
    @SuppressLint("NewApi")
    private void UpdateAndSortResults(ClassifiedImages result) {

        if (result.getImages() != null) {
            List<ClassifierResult> resultClasses = result.getImages().get(0).getClassifiers();
            if (resultClasses.size() > 0) {
                ClassifierResult classifier = resultClasses.get(0);
                List<ClassResult> classList = classifier.getClasses();
                if (classList.size() > 0) {

                    // Sort the results
                    // todo use another way to sort.
                    classList.sort(new Comparator<ClassResult>() {
                        @Override
                        public int compare(ClassResult o1, ClassResult o2) {
                            return -(o1.getScore().compareTo(o2.getScore()));
                        }
                    });

                    // Removes all of the elements from resArr.
                    resArr.clear();

                    for (int i = 0; i < classList.size(); i++) {

                        // Create a temp result object from each result.
                        Result tempRes = new Result(classList.get(i).getClassName(), classList.get(i).getScore());
                        resArr.add(tempRes);
                    }

                    // Continue only if there are any results.
                    if (!resArr.isEmpty()) {

                        // Performs a set of filters on the results.
                        filterResArr = filter.startFiltering(resArr);

                        customAdapter = null;
                        customAdapter = new CustomArrayAdapter(this, R.layout.activity_listview, resArr, filterResArr);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Show the sort result on screen.
                                resListView.setAdapter(customAdapter);

                                // Speak the relevant results that passed the filter
                                startSpeak();
                            }
                        });
                    }
                }
            }
        }
    }

} // End MainActivity