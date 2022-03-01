package com.example.twofactor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Face_learning extends AppCompatActivity implements SurfaceHolder.Callback{

    private Camera camera;
    private MediaRecorder mediaRecorder;
    private Button btn, btn_choose, btn_upload;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Uri filePath;
    private boolean recording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_learning);



        TedPermission.with(this)
                .setPermissionListener(permission).setRationaleMessage("녹화를 위해 권한을 허용해주세요.")
                .setDeniedMessage("권한이 거부되었습니다. 설정 > 권한에서 허용해주세요.")
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
                .check();
//        String sdcard= Environment.getExternalStorageDirectory().getAbsolutePath();
//        filename=sdcard+ File.separator+"recorded.mp4";


        btn = (Button)findViewById(R.id.camera);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recording){
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    recording = false;

                    MediaScanner ms = MediaScanner.newInstance(Face_learning.this);
                    try {
                        btn.setText("녹화시작");
                        // TODO : 미디어 스캔
                        ms.mediaScanning("/sdcard/DCIM/Camera");
                    } catch (Exception e) {
                        e.printStackTrace(); Log.d("MediaScan", "ERROR" + e);
                    } finally { }

                } else {
                    btn.setText("녹화중");
                    Toast.makeText(getApplicationContext(),"촬영이 끝나면 녹화중 버튼을 누르세요",Toast.LENGTH_LONG).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            Toast.makeText(Face_learning.this,"녹화가 시작되었습니다",Toast.LENGTH_SHORT).show();
                            try {
                                mediaRecorder = new MediaRecorder();
                                camera.unlock();
                                mediaRecorder.setCamera(camera);
                                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
                                mediaRecorder.setOrientationHint(270);
                                mediaRecorder.setOutputFile("/sdcard/DCIM/Camera/test.mp4");
                                mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
                                mediaRecorder.prepare();
                                mediaRecorder.start();
                                recording = true;


                            }catch (Exception e){
                                e.printStackTrace();
                                mediaRecorder.release();
                            }
                        }
                    });
                }
            }
        });

        btn_choose = (Button)findViewById(R.id.bt_choose);
        btn_upload = (Button)findViewById(R.id.bt_upload);

        btn_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"동영상을 선택하세요"),0);
            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });



        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data){
            super.onActivityResult(requestCode,resultCode,data);
            if(requestCode ==0 && resultCode ==RESULT_OK){
                filePath = data.getData();
                Log.d("TAG", "uri: "+String.valueOf(filePath));
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,filePath));
//                try{
//                    Bitmap bitmap = MediaStore.Video.Media.getContentUri(getContentResolver(),filePath);
//
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
            }
        }

        private void uploadFile(){
        if(filePath != null){
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("업로드중...");
            progressDialog.show();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            String getCompany_num = getIntent().getStringExtra("companyNum");

//            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMHH_mmss");
//            Date now= new Date();
//            String filename = formatter.format(now)+".mp4";
            StorageReference storageReference = storage.getReferenceFromUrl("gs://twofactor-d19ed.appspot.com/").child(getCompany_num+".mp4");
            storageReference.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),"업로드 완료",Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),"업로드 실패", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    @SuppressWarnings("VisibleForTest")
                            double progress = (100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploaded" + ((int)progress)+"% ...");
                }
            });

        }else {
            Toast.makeText(getApplicationContext(),"파일을 먼저 선택하세요.",Toast.LENGTH_SHORT).show();
        }
        }

        PermissionListener permission = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(Face_learning.this, "권한 허가",Toast.LENGTH_SHORT).show();
                camera = Camera.open(1);
                camera.setDisplayOrientation(90);
                surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
                surfaceHolder =surfaceView.getHolder();
                surfaceHolder.addCallback(Face_learning.this);
                surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(Face_learning.this, "권한 거부",Toast.LENGTH_SHORT).show();

            }
        };


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    private void refreshCamera(Camera camera) {
        if(surfaceHolder.getSurface() == null){
            return;
        }
        try{
            camera.stopPreview();
        }catch (Exception e){
            e.printStackTrace();
        }

        setCamera(camera);
    }

    private void setCamera(Camera cam) {
        camera = cam;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refreshCamera(camera);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


}
