package com.example.twofactor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGSaver;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    EditText ed_name, ed_PhoneNum, ed_CompanyNum;
    LinearLayout linearLayout;
    Button button1;
    private Uri filePath;
    private DatabaseReference mDatabase,mPostReference;
    private RadioGroup radioGroup;
    private RadioButton r_btn1, r_btn2;
    public DatabaseReference key;

    String num;
    static ArrayList<String> arrayIndex = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ed_name = findViewById(R.id.name);
        ed_PhoneNum = findViewById(R.id.phoneNum);
        ed_CompanyNum = findViewById(R.id.companyNum);
        r_btn1 = findViewById(R.id.companyGroup);
        r_btn2 = findViewById(R.id.non_companyGroup);
        radioGroup = findViewById(R.id.radioGroup);
        linearLayout = findViewById(R.id.num_layout);
        button1 = (Button) findViewById(R.id.toCheck);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId ==R.id.companyGroup){
                    linearLayout.setVisibility(View.VISIBLE);
                }else {
                    linearLayout.setVisibility(View.INVISIBLE);
                }
            }
        });

        mPostReference = FirebaseDatabase.getInstance().getReference("usersNum");

        mPostReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

//                arrayIndex.clear();
                for(DataSnapshot datasnapshot :snapshot.getChildren()){
//                    num = datasnapshot.child("companyNum").getValue(String.class);
                    num = datasnapshot.getValue().toString();
                    arrayIndex.add(num);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //사번 존재여부 확인 버튼
        final Button button = (Button)findViewById(R.id.check);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!IsExistNum()){
                    Toast.makeText(MainActivity.this, "등록된 사용자가 아닙니다.",Toast.LENGTH_SHORT).show();
                    button1.setClickable(false);
                }else{
                    Toast.makeText(MainActivity.this, "등록된 사용자 입니다.",Toast.LENGTH_SHORT).show();
                    button1.setClickable(true);
                }

            }
        });

        //user테이블에 정보 저장하는 버튼
        mDatabase = FirebaseDatabase.getInstance().getReference();
        key = mDatabase.child("user").push();

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ed_name.getText().toString().length() == 0 || ed_PhoneNum.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "정보를 다시 입력하세요!", Toast.LENGTH_SHORT).show();
                    button1.setClickable(false);
                }else {
                    button1.setClickable(true);
                }

                String getUserName = ed_name.getText().toString();
                String getUserPhoneNum = ed_PhoneNum.getText().toString();
               // String getCompanyNum = ed_CompanyNum.getText().toString();
                String getCompanyNum;

                if(ed_CompanyNum.getText().toString().length()==0){
                    long now = System.currentTimeMillis();
                    SimpleDateFormat formatter = new SimpleDateFormat("MMHHmmss");
                    Date date= new Date(now);
                    getCompanyNum =formatter.format(date);
                }else {
                    getCompanyNum = ed_CompanyNum.getText().toString();
                }


                HashMap result = new HashMap<>();
                result.put("name", getUserName);
                result.put("PhoneNum", getUserPhoneNum);
                result.put("companyNum", getCompanyNum);

                writeNewUser(getCompanyNum, getUserName, getUserPhoneNum);
//                uploadFile(ed_CompanyNum.getText().toString());
            }
        });
    }

    private void writeNewUser(final String userId, String name, String num) {
        User user = new User(name, num, userId);
        mDatabase.child("user").child(userId).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, "저장을 완료했습니다", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), Face_learning.class);
                intent.putExtra("companyNum", userId);
                startActivity(intent);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "저장을 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //request코드가 0이고 OK를 선택했고 data에 뭔가가 들어 있다면
        if (requestCode == 0 && resultCode == RESULT_OK) {
            filePath = data.getData();
            Log.d(TAG, "uri:" + String.valueOf(filePath));
//            try {
//                //Uri 파일을 Bitmap으로 만들어서 ImageView에 집어 넣는다.
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
//                iv.setImageBitmap(bitmap);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public boolean IsExistNum(){ //usersNum 테이블에 사번 존재여부 확인하는 함수
        boolean IsExist = arrayIndex.contains(ed_CompanyNum.getText().toString());
        return IsExist;
    }


    private void uploadFile(String userID) {




//        if(filePath != null){
//            final ProgressDialog progressDialog = new ProgressDialog(this);
//            progressDialog.setTitle("업로드중...");
//            progressDialog.show();
//
//            FirebaseStorage storage = FirebaseStorage.getInstance();
//
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMHH_mmss");
//            Date now= new Date();
//            String filename = formatter.format(now)+".png";
//            StorageReference storageReference = storage.getReferenceFromUrl("gs://twofactor-d19ed.appspot.com/").child(userID).child(filename);
//            storageReference.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                    progressDialog.dismiss();
//                    Toast.makeText(getApplicationContext(),"업로드 완료",Toast.LENGTH_SHORT).show();
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    progressDialog.dismiss();
//                    Toast.makeText(getApplicationContext(),"업로드 실패", Toast.LENGTH_SHORT).show();
//                }
//            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
//                    @SuppressWarnings("VisibleForTest")
//                    double progress = (100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
//                    progressDialog.setMessage("Uploaded" + ((int)progress)+"% ...");
//                }
//            });
//
//        }else {
//            Toast.makeText(getApplicationContext(),"파일을 먼저 선택하세요.",Toast.LENGTH_SHORT).show();
//        }
//    }

//    public String getFilePathFormUri(Uri uri){
//        String path = null;
//        String[] data = {MediaStore.Files.FileColumns.DATA};
//        Cursor cur = getContentResolver().query(uri, data,null,null,null);
//
//        if(cur.moveToFirst()){
//            int col = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            path = cur.getString(col);
//        }
//        cur.close();
//        return  path;
//    }

    }
}

