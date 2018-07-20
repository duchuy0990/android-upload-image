package com.example.duchuynm.uploadimage;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ftpConnection extends AsyncTask {
    private final String hostname = "ftp.dlptest.com";         //"test.rebex.net"
    private final String userName = "dlpuser@dlptest.com";         //"demo"
    private final String password = "3D6XZV9MKdhM5fF";        //"password"
    private String filePath;
    private Activity context;
    private FTPClient ftpClient = null;

    private boolean login = false;
    boolean storeFile = false;

    public ftpConnection(String filePath, Activity context) {
        this.filePath = filePath;
        this.context = context;
    }

    public boolean isSuccess() {
        return storeFile;
    }

    public void connect() {
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(hostname);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("connect",ftpClient.getReplyString());
        try {
            login = ftpClient.login(userName,password);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("login",ftpClient.getReplyString());
    }

    private void ftpConfig() {
        ftpClient.enterLocalPassiveMode();
        ftpClient.setDataTimeout(60);
        try {
            ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            //ftpClient.changeWorkingDirectory("/UploadImage/img");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE,FTP.BINARY_FILE_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean upload() {
        FileInputStream fileInputStream = null;

        try {
            connect();
            ftpConfig();

            File image = new File(filePath);
            fileInputStream = new FileInputStream(image);

            storeFile = ftpClient.storeFile(image.getName(),fileInputStream);

            Log.e("storeFile", String.valueOf(storeFile));

            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(login) {
                    ftpClient.logout();
                }
                if(ftpClient != null) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return storeFile;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        boolean success = upload();
        return (boolean)success;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if((boolean)o) {
            Toast.makeText(context,"successfully",Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(context,"loi khong xac dinh, vui long thu lai",Toast.LENGTH_LONG).show();
        }
    }
}
