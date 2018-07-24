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
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    Button chooseImage, uploadImageToServer;
    EditText imageName;
    ImageView imageSelected;

    private int REQUEST_CODE_GALLERY = 9999;
    private int REQUEST_CODE_CAMERA = 9998;

    Bitmap bitmap;

    String getTextFromEditText = " ";

    private String deviceName;
    private java.sql.Date currentTime;

    ProgressDialog progressDialog;


    private void init() {
        chooseImage = findViewById(R.id.btnChooseImage);
        uploadImageToServer = findViewById(R.id.btnUploadtoServer);
        imageName = findViewById(R.id.edtImageName);
        imageSelected = findViewById(R.id.imageView);
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
            getDataToUpLoad();
            final String filePath = saveImage(getTextFromEditText, bitmap);

            Log.e("name", getTextFromEditText);
            Log.e("time", currentTime.toString());
            Log.e("device", deviceName);
            Log.e("filePath", filePath);

            File file = new File(filePath);

            final ftpConnection ftpconn = new ftpConnection(
                    filePath,
                    MainActivity.this) {

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
                "Camera" };
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
                        }
                    }
                });
        pictureDialog.show();
    }

    public String saveImage(String fileName,Bitmap b) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        displayImageSelected(requestCode,resultCode,data);
        imageName.setText("");
    }

    private void visibleButton() {
        imageName.setVisibility(View.VISIBLE);
        uploadImageToServer.setVisibility(View.VISIBLE);
        imageSelected.setVisibility(View.VISIBLE);
    }

    private void displayImageSelected(int requestCode, int resultCode, Intent data) {
        if(check(requestCode,resultCode)) {
            visibleButton();
            if(requestCode == REQUEST_CODE_CAMERA) bitmap = (Bitmap) data.getExtras().get("data");
            else {
                Uri imageUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            imageSelected.setImageBitmap(bitmap);
        }
    }

    private boolean check(int requestCode, int resultCode) {
        return (requestCode == REQUEST_CODE_CAMERA || requestCode == REQUEST_CODE_GALLERY)
                && resultCode == RESULT_OK;
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
