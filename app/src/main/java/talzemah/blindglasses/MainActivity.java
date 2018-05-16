package talzemah.blindglasses;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_IMAGE_PATH = 3;

    private static final int TIME_BETWEEN_CAPTURES = 15;

    private VisualRecognition visualRecognition;
    private TextToSpeech textToSpeech;
    private ResultsFilter filter;

    private Button captureImageBtn;
    private Button selectImageBtn;
    private ImageView resultImageView;
    private ListView resListView;
    private ProgressBar progressBar;
    private Timer timer;
    private Date captureTime;

    private File currentPhotoFile;
    private CustomArrayAdapter customAdapter;
    private ArrayList<Result> currentResArr;
    private ArrayList<Result> filterResArr;

    private Boolean isFirstClick = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen always on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create VisualRecognition Object.
        visualRecognition = new VisualRecognition("2018-03-19");
        visualRecognition.setApiKey(getString(R.string.VisualRecognitionApiKey));

        // Create TextToSpeech Object (Android).
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Set language to speech.
                    int languageResult = textToSpeech.setLanguage(Locale.ENGLISH);
                    // Verifies that language is supported and available.
                    if (languageResult == TextToSpeech.LANG_NOT_SUPPORTED || languageResult == TextToSpeech.LANG_MISSING_DATA) {
                        Toast.makeText(MainActivity.this, textToSpeech.getVoice().getLocale() + " language is not supported", Toast.LENGTH_SHORT).show();
                        textToSpeech.setLanguage(Locale.getDefault());
                    } else {
                        // TextToSpeech initialization success.

                        enableButtons();

                        // Set custom parameters.
                        // Sets the speech rate.
                        textToSpeech.setSpeechRate(0.7f);
                        // Sets the speech pitch for the TextToSpeech engine.
                        textToSpeech.setPitch(0.9f);
                    }

                } else {
                    // TextToSpeech initialization Failed.
                    Toast.makeText(MainActivity.this, "TextToSpeech initialization error", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

        // Create results filter object.
        filter = new ResultsFilter();

        // ImageView to show the captured image.
        resultImageView = (ImageView) findViewById(R.id.imageView_result);

        // Show the results.
        resListView = (ListView) findViewById(R.id.ListView_results);

        // Progress Bar.
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // Initialize the ArrayList.
        currentResArr = new ArrayList<>();
        filterResArr = new ArrayList<>();

        // Camera button.
        captureImageBtn = (Button) findViewById(R.id.Btn_CaptureImage);
        captureImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startCameraActivity();
            }
        });

        // Gallery Button.
        selectImageBtn = (Button) findViewById(R.id.Btn_SelectImage);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /// takePictureFromCamera(REQUEST_GALLERY);
            }
        });

    } // End onCreate.

    @Override
    protected void onResume() {
        super.onResume();

        // Performs the first click automatically for the blind user.
        if (isFirstClick) {
            captureImageBtn.performClick();
            isFirstClick = false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (timer != null)
            timer.cancel();
    }

    private void startTimer() {

        timer = new Timer();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                // Calculate elapsed time in seconds.
                Date currentTime = Calendar.getInstance().getTime();
                int elapsedTime = (int) ((currentTime.getTime() - captureTime.getTime()) / 1000);

                if (!textToSpeech.isSpeaking() && elapsedTime > TIME_BETWEEN_CAPTURES) {
                    timer.cancel();
                    timer = null;
                    startCameraActivity();
                }
            }
        };

        timer.schedule(timerTask, 1000, 1000);
    }


    private void enableButtons() {
        if (textToSpeech != null && visualRecognition != null) {
            captureImageBtn.setEnabled(true);
            selectImageBtn.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop speech when App is going into the background.
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release resources of textToSpeech when App is closed.
        if (textToSpeech != null)
            textToSpeech.shutdown();


        if (visualRecognition != null)
            visualRecognition = null;
    }

    // speak the final results after sort and filtering.
    private void speak() {

        for (int i = 0; i < filterResArr.size(); i++) {
            textToSpeech.speak(filterResArr.get(i).getname(), TextToSpeech.QUEUE_ADD, null, null);
        }

        startTimer();
    }

    private void startCameraActivity() {

        if (progressBar.getVisibility() == View.GONE && timer == null) {
            // Go to camera activity.
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivityForResult(intent, REQUEST_IMAGE_PATH);
        }
    }


    // Invoke When user come back from camera to the application.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Make sure there is no some problem.
        if (resultCode == RESULT_OK) {

            switch (requestCode) {
                // todo delete
                case REQUEST_IMAGE_CAPTURE:
                    showAndAnalyzeImage();

                    break;
                // todo delete
                case REQUEST_GALLERY:
                    showAndAnalyzeImage();

                    break;

                case REQUEST_IMAGE_PATH:

                    // Get current time to measure time between image samples.
                    captureTime = Calendar.getInstance().getTime();

                    deleteOldResults();

                    // Update currentPhotoFile path.
                    String path = data.getStringExtra("imagePath");

                    if (path != null) {
                        currentPhotoFile = new File(path);
                        showAndAnalyzeImage();
                    }

                    break;
            }
        }
    }

    private void deleteOldResults() {

        resListView.setAdapter(null);
    }

    private void showAndAnalyzeImage() {

//        File temp = compressImage(currentPhotoFile.getAbsolutePath());
//        currentPhotoFile = temp;

        compressImage();

        // Show the captured image in resultImageView.
        Picasso.with(this)
                .load(currentPhotoFile)
                .into(resultImageView);

        usingVisualRecognition();
    }

    private void usingVisualRecognitionDemo() {

        progressBar.setVisibility(View.VISIBLE);

        //compressImage2();

        currentResArr.clear();

        currentResArr.add(new Result("a", 0.9f));
        currentResArr.add(new Result("b", 0.8f));
        currentResArr.add(new Result("c", 0.7f));
        currentResArr.add(new Result("d", 0.6f));
        currentResArr.add(new Result("e", 0.5f));
        currentResArr.add(new Result("g color", 0.9f));
        currentResArr.add(new Result("h color", 0.9f));
        currentResArr.add(new Result("i color", 0.9f));
        currentResArr.add(new Result("j color", 0.9f));

        // Continue only if there are any results.
        if (!currentResArr.isEmpty()) {

            // Performs a set of filters on the results.
            filterResArr = filter.startFiltering(currentResArr);

            customAdapter = null;
            customAdapter = new CustomArrayAdapter(this, R.layout.activity_listview, currentResArr, filterResArr);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Show the sort result on screen.
                    resListView.setAdapter(customAdapter);

                    // Speak the relevant results that passed the filter
                    speak();
                }
            });
        }

        progressBar.setVisibility(View.GONE);

    }

    // Analyzes what is in the picture.
    private void usingVisualRecognition() {

        progressBar.setVisibility(View.VISIBLE);

        InputStream imagesStream = null;

        try {
            imagesStream = new FileInputStream(currentPhotoFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Create custom classifier.
        ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                .imagesFile(imagesStream)
                .imagesFilename(currentPhotoFile.getName())
                .build();

        // Asynchronous Request
        visualRecognition.classify(classifyOptions).enqueue(new ServiceCallback<ClassifiedImages>() {

            @Override
            public void onResponse(ClassifiedImages response) {

                // Delete last image from device.
                currentPhotoFile.delete();

                // Here we are in another thread - every Change on GUI must made by UI thread.
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

                // Delete last image from device.
                currentPhotoFile.delete();

                // Here we are in another thread - every Change on GUI must made by UI thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Error while processing the image!", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.d(TAG, e.toString());

                // todo speak appropriate message.
                // todo startCameraActivity();
            }
        });
    }


    // Get the result object from VR, sort the element and update the currentResArr.
    @SuppressLint("NewApi")
    private void UpdateAndSortResults(ClassifiedImages result) {

        // Get all results.
        if (result.getImages() != null) {
            List<ClassifierResult> resultClasses = result.getImages().get(0).getClassifiers();
            if (resultClasses.size() > 0) {
                ClassifierResult classifier = resultClasses.get(0);
                List<ClassResult> classList = classifier.getClasses();
                if (classList.size() > 0) {
                    // Continue only if there is at list one result or more.

                    // Sort the results
                    // todo use another way to sort.
                    classList.sort(new Comparator<ClassResult>() {
                        @Override
                        public int compare(ClassResult o1, ClassResult o2) {
                            return -(o1.getScore().compareTo(o2.getScore()));
                        }
                    });

                    // Delete previous results.
                    currentResArr.clear();

                    // Update current results.
                    for (int i = 0; i < classList.size(); i++) {
                        Result tempRes = new Result(classList.get(i).getClassName(), classList.get(i).getScore());
                        currentResArr.add(tempRes);
                    }

                    // todo check if necessary.
                    // Continue only if there are any results.
                    if (!currentResArr.isEmpty()) {

                        // Performs a set of filters on the results.
                        filterResArr = filter.startFiltering(currentResArr);

                        customAdapter = null;
                        customAdapter = new CustomArrayAdapter(this, R.layout.activity_listview, currentResArr, filterResArr);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Show the sort result on screen.
                                resListView.setAdapter(customAdapter);

                                // Speak the relevant results that passed the filter
                                speak();
                            }
                        });
                    }
                }
            }
        }
    }

    public void compressImage() {

        String imagePath = getRealPathFromURI(currentPhotoFile.getAbsolutePath());
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        // by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        // you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(imagePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        // max Height and width values of the compressed image is taken as 816x612
        float maxHeight = 1080.0f;
        float maxWidth = 1920.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        // width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        // setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

        // inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        // this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
            // load the bitmap from its path
            bmp = BitmapFactory.decodeFile(imagePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        // check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(imagePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }

            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        File file = getFilename();
        try {
            out = new FileOutputStream(file);

            // write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        currentPhotoFile.delete();

        currentPhotoFile = file;
        refreshGallery(currentPhotoFile);
        // todo boolean?
    }

    private String getRealPathFromURI(String contentURI) {

        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);

        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public File getFilename() {

        // Create directory.
        File appDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/BlindGlasses");
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();
        }

        // Create file name.
        String sourceFileName = currentPhotoFile.getName();
        String fileName = "Comp_" + sourceFileName;
        File file = new File(appDirectory, fileName);

        return file;
    }

    // Show the capture image in gallery.
    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);

        Log.d(TAG, "Image added to gallery");
    }

} // End MainActivity