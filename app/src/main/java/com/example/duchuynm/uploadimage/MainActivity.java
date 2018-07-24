package com.example.duchuynm.uploadimage;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    Button chooseImage, uploadImageToServer;
    EditText imageName;
    ImageView imageSelected;
    FrameLayout frameLayout;

    private int REQUEST_CODE_GALLERY = 9999;
    private int REQUEST_CODE_CAMERA = 9998;
    private int REQUEST_CODE_VIDEO = 9997;

    Bitmap bitmap = null;

    String getTextFromEditText = " ";

    private String deviceName;
    private java.sql.Date currentTime;

    ProgressDialog progressDialog;
    private VideoView videoSelected;
    private Uri videoUri = null;


    private void init() {
        chooseImage = findViewById(R.id.btnChooseImage);
        uploadImageToServer = findViewById(R.id.btnUploadtoServer);
        imageName = findViewById(R.id.edtImageName);
        imageSelected = findViewById(R.id.imageView);
        videoSelected = findViewById(R.id.videoView);
        frameLayout = findViewById(R.id.frame_layout_video);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });

        uploadImageToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTextFromEditText = imageName.getText().toString().trim();
                if(getTextFromEditText.length() > 0) {
                    uploadImageToServer();
                }
                else Toast.makeText(MainActivity.this,"Field name can't empty",Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void uploadImageToServer() {
        if(!checkInternet()) {
            Toast.makeText(MainActivity.this,"Internet chua duoc bat",Toast.LENGTH_LONG).show();
        }
        else {
            String filePath;
            getDataToUpLoad();
            if(bitmap != null) {
                filePath = saveImage(getTextFromEditText, bitmap);
            }
            else {
                filePath = saveVideo(getTextFromEditText, videoUri);
            }

            Log.e("name", getTextFromEditText);
            Log.e("time", currentTime.toString());
            Log.e("device", deviceName);
            Log.e("filePath", filePath);

            File file = new File(filePath);

            final ftpConnection ftpconn = new ftpConnection(
                    filePath) {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    showProgressDialog();
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    progressDialog.dismiss();
                    if((boolean)o) {
                        Toast.makeText(MainActivity.this,"successfully",Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(MainActivity.this,"loi khong xac dinh",Toast.LENGTH_LONG).show();
                    }
                }
            };
            ftpconn.execute();

            JDBCconnection jdbcConnection = new JDBCconnection();
            jdbcConnection.execute(file.getName(), currentTime.toString(), deviceName);

            imageName.setText("");
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(
                MainActivity.this,
                ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("please wait...");
        progressDialog.show();
    }

    private void showPictureDialog() {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Photo Gallery",
                "Camera",
                "Video"};
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallery();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                            case 2:
                                takeVideo();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    private void takeVideo() {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(videoIntent,REQUEST_CODE_VIDEO);
    }

    public String saveImage(String fileName, Bitmap b) {
        File image = null;
        String path = Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/duchuy";
        final File Dir = new File(path);
        if(!Dir.exists()) {
            Dir.mkdir();
        }

        fileName = ""+fileName+getTime();

        try {
                image = File.createTempFile(
                        fileName,
                        ".jpg",
                        Dir);
                FileOutputStream fos = new FileOutputStream(image);
                b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return image.getAbsolutePath();
    }

    public String saveVideo(String fileName, Uri uri) {
        File video = null;
        String path = Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/duchuy";
        final File Dir = new File(path);
        if(!Dir.exists()) {
            Dir.mkdir();
        }

        fileName = ""+fileName+getTime();

        try {
            video = File.createTempFile(
                    fileName,
                    ".mp4",
                    Dir);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(video);

            byte[] b = new byte[1024];
            int len;

            while ((len = inputStream.read(b)) >0 ) {
                fos.write(b,0,len);
            }
            inputStream.close();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return video.getAbsolutePath();
    }

    public String getTime() {
        String format = "yyyyMMddhhmmss";
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = Calendar.getInstance().getTime();
        String getTime = df.format(date).toString();
        return getTime;
    }

    private void takePhotoFromCamera() {
        Intent accessCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(accessCameraIntent,REQUEST_CODE_CAMERA);
    }

    private void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent,REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if((requestCode == REQUEST_CODE_CAMERA) && (resultCode == RESULT_OK)) {
            visibleButton();
            displayImageReturnedFromCamera(data);
        }
        else if(requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK) {
            visibleButton();
            displayImageReturnedFromGallery(data);
        }
        else if(requestCode == REQUEST_CODE_VIDEO && resultCode == RESULT_OK) {
            displayVideo();
            visibleButton();
            displayVideoReturnedFromCamera(data);
            videoUri = data.getData();
        }
        imageName.setText("");
    }

    private void displayVideoReturnedFromCamera(Intent data) {
        Uri uri = Uri.parse(data.getData().toString());
        videoSelected.setVideoURI(uri);
        displayVideo();
    }

    private void visibleButton() {
        imageName.setVisibility(View.VISIBLE);
        uploadImageToServer.setVisibility(View.VISIBLE);
    }

    private void displayImageReturnedFromCamera(Intent data) {
        displayImage();
        bitmap = (Bitmap) data.getExtras().get("data");
        imageSelected.setImageBitmap(bitmap);
    }

    private void displayImage() {
        imageSelected.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        imageSelected.setLayoutParams(layoutParams);
    }

    private void displayVideo() {
        frameLayout.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        videoSelected.setLayoutParams(params);
        MediaController mediaController = new MediaController(MainActivity.this);
        mediaController.setAnchorView(videoSelected);
        videoSelected.setMediaController(mediaController);
    }

    private void displayImageReturnedFromGallery(Intent data) {
        Uri imageUri = data.getData();
        try {
            displayImage();
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageSelected.setImageBitmap(bitmap);
    }

    public String getDeviceName() {
        return Build.DEVICE;
    }

    public void getDataToUpLoad() {
        deviceName = getDeviceName();
        currentTime = new java.sql.Date(System.currentTimeMillis());
        getTextFromEditText = imageName.getText().toString();
    }
}
