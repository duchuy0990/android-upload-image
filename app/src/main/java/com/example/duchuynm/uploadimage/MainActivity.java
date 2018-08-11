package com.example.duchuynm.uploadimage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
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

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    Button chooseImage, uploadImageToServer;
    EditText imageName;
    ImageView imageSelected;
    MediaController mediaController = null;
    FrameLayout frameLayout;
    ProgressDialog progressDialog;
    private VideoView videoSelected;
    MenuItem optionMenu;

    private static final String folder = Environment
            .getExternalStorageDirectory()
            .toString()
            +"/UploadImage";
    private static final int REQUEST_CODE_GALLERY = 9999;
    private static final int REQUEST_CODE_CAMERA = 9998;
    private static final int REQUEST_CODE_VIDEO = 9997;
    private static final int PIC_EDIT = 9995;
    private int TYPE_IMAGE = 0;
    private int TYPE_VIDEO = 1;

    Bitmap bitmap = null;

    String getTextFromEditText = "";
    private String deviceName;
    private java.sql.Date currentTime;
    private Uri uri = null;
    private String path;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemEdit:
                startActionEdit();
        }
        return super.onOptionsItemSelected(item);
    }

    private void startActionEdit() {
        Intent editIntent = new Intent(
                MainActivity.this,
                DsPhotoEditorActivity.class);

        editIntent.setData(uri);

        editIntent.putExtra(
                DsPhotoEditorConstants.DS_PHOTO_EDITOR_API_KEY,
                "7fdf6963cfbf09a9a14e43c2fcd01af279ae7638");

        editIntent.putExtra(
                DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY,
                "UploadImage");

        startActivityForResult(editIntent,PIC_EDIT);
    }

    @SuppressLint("ResourceType")
    private void init() {
        chooseImage = findViewById(R.id.btnChooseImage);
        uploadImageToServer = findViewById(R.id.btnUploadtoServer);
        imageName = findViewById(R.id.edtImageName);
        imageSelected = findViewById(R.id.imageView);
        videoSelected = findViewById(R.id.videoView);
        frameLayout = findViewById(R.id.frame_layout_video);
        frameLayout.setBackgroundColor(getResources().getColor(R.color.black));
        optionMenu = findViewById(R.menu.my_menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showActionDialog();
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
        if (!checkInternet()) {
            Toast.makeText(MainActivity.this, "Internet chua duoc bat", Toast.LENGTH_LONG).show();
        } else {
            getDataToUpLoad();

            final ftpConnection ftpconn = new ftpConnection(path) {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    showProgressDialog();
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    progressDialog.dismiss();
                    if ((boolean) o) {
                        Toast.makeText(MainActivity.this, "successfully", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "loi khong xac dinh", Toast.LENGTH_LONG).show();
                    }
                    Intent i = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(i);
                }
            };
            ftpconn.execute();

            File file = new File(path);

            JDBCconnection jdbcConnection = new JDBCconnection();
            jdbcConnection.execute(file.getName(), currentTime.toString(), deviceName);

            imageName.setText("");
        }
    }

    private void askPermissionAccessCamera(int requestCode) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int readPermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            int accessCameraPermission = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA);

            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED ||
                    accessCameraPermission != PackageManager.PERMISSION_GRANTED)
            {
                this.requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                        },
                        requestCode
                );
                return;
            }
        }
        this.takePhotoFromCamera();
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE_CAMERA: {
                if(grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    takePhotoFromCamera();
                }
            }

            case REQUEST_CODE_VIDEO: {
                if(grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    takeVideo();
                }
            }

            case REQUEST_CODE_GALLERY: {
                if(grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    choosePhotoFromGallery();
                }
            }



        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(
                MainActivity.this,
                ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("please wait...");
        progressDialog.show();
    }

    private void showActionDialog() {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Photo Gallery",
                "Camera",
                "Video"
        };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                if(Build.VERSION.SDK_INT >= 23) {
                                    askPermissionAccessCamera(REQUEST_CODE_GALLERY);
                                }
                                else {
                                    choosePhotoFromGallery();
                                }
                                break;
                            }
                            case 1: {
                                if(Build.VERSION.SDK_INT >= 23) {
                                    askPermissionAccessCamera(REQUEST_CODE_CAMERA);
                                }
                                else {
                                    takePhotoFromCamera();
                                }
                                break;
                            }
                            case 2: {
                                if(Build.VERSION.SDK_INT >= 23) {
                                    askPermissionAccessCamera(REQUEST_CODE_CAMERA);
                                }
                                else {
                                    takeVideo();
                                }
                                break;
                            }
                        }
                    }
                });
        pictureDialog.show();
    }

    public String getTime() {
        String format = "yyyyMMddhhmmss";
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = Calendar.getInstance().getTime();
        String getTime = df.format(date).toString();
        return getTime;
    }

    private File createFile(int type) {
        String name = getTime();
        File dir = new File(folder);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        File des;
        if(type == TYPE_IMAGE) {
            des = new File(folder.toString()+"/"+name+".jpg");
            return des;
        }
        else if(type == TYPE_VIDEO) {
            des = new File(folder.toString()+"/"+name+".mp4");
            return des;
        }
        return null;
    }

    private void takePhotoFromCamera() {
        Intent accessCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File des = createFile(0);
        Uri uri = Uri.fromFile(des);
        accessCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
        path = des.getAbsolutePath();
        startActivityForResult(accessCameraIntent,REQUEST_CODE_CAMERA);
    }

    private void takeVideo() {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        File des = createFile(1);
        Uri uri = Uri.fromFile(des);
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
        path = des.getAbsolutePath();
        startActivityForResult(videoIntent,REQUEST_CODE_VIDEO);
    }

    private void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/* video/*");
        startActivityForResult(galleryIntent,REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            if(requestCode == PIC_EDIT) {
                Uri uri = data.getData();
                imageSelected.setImageURI(uri);
            }
            if(requestCode == REQUEST_CODE_CAMERA) {
                setDataImageView(path);
                displayImage();
                visibleButton();
            }
            else if(requestCode == REQUEST_CODE_GALLERY) {
                uri = data.getData();
                visibleButton();
                displayImageOrVideoReturnedFromGallery(data);
            }
            else if(requestCode == REQUEST_CODE_VIDEO) {
                uri = data.getData();
                displayVideo();
                visibleButton();
                setDataVideoView();
            }
        }

        imageName.setText("");
    }

    private void setDataVideoView() {
        videoSelected.setVideoPath(path);
    }

    private void visibleButton() {
        imageName.setVisibility(View.VISIBLE);
        uploadImageToServer.setVisibility(View.VISIBLE);
    }

    private void setDataImageView(String data) {
        bitmap = BitmapFactory.decodeFile(data);
        imageSelected.setImageBitmap(bitmap);
    }

    private void invisibleVideoView() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = 0;
        layoutParams.weight = 0;
    }

    private void invisibleImageView() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageSelected.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = 0;
        layoutParams.weight = 0;
    }

    private void visibleImageView() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageSelected.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = 0;
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.weight = 7.0f;
        imageSelected.setLayoutParams(layoutParams);
    }

    private void visibleVideoView() {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.height = 0;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.weight = 7.0f;
        mediaController = new MediaController(MainActivity.this);
        mediaController.setAnchorView(frameLayout);
        videoSelected.setMediaController(mediaController);
    }

    private void displayVideo() {
        invisibleImageView();
        visibleVideoView();
    }

    private void displayImage() {
        invisibleVideoView();
        visibleImageView();
    }

    private void displayImageOrVideoReturnedFromGallery(Intent data) {
        Uri uri = data.getData();
        String _path = uri.getPath().toString();
        String type = _path.substring(_path.indexOf("external")+9,_path.indexOf("/media"));
        try {
            if(type.equals("images")) {
                displayImage();
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                File des = createFile(TYPE_IMAGE);
                OutputStream desStream = new FileOutputStream(des);
                InputStream srcStream = this.getContentResolver().openInputStream(uri);
                writeData(srcStream,desStream);
                imageSelected.setImageBitmap(bitmap);
            }
            else if(type.equals("video")) {
                File des = createFile(TYPE_VIDEO);
                OutputStream desStream = new FileOutputStream(des);
                InputStream srcStream = this.getContentResolver().openInputStream(uri);
                writeData(srcStream,desStream);
                displayVideo();
                videoSelected.setVideoURI(uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeData(InputStream src, OutputStream des) {
        byte[] bytes = new byte[1024];
        int len = 0;
        try {
            while((len =src.read(bytes))>0) {
                des.write(bytes,0,len);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
