package com.example.twofactor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.executor.TaskExecutor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.firebase.ui.auth.ui.phone.PhoneNumberVerificationHandler;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Otp extends AppCompatActivity {

    Button phone_check, otp_check;
    EditText phone_num, otp_num;
    String  user_phoneNum;
    String getCompany_num, phoneNumber,otpNumber;

    String num;
    static ArrayList<String> arrayIndex = new ArrayList<String>();

    private DatabaseReference mPostReference, userRef;

    FirebaseAuth auth  = FirebaseAuth.getInstance();
    private String verificationCode;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        phone_check = findViewById(R.id.phone_check);
        otp_check = findViewById(R.id.otp_check);
        otp_num = findViewById(R.id.otp);
        phone_num = findViewById(R.id.phone);

        getCompany_num = getIntent().getStringExtra("userID");

        mPostReference = FirebaseDatabase.getInstance().getReference("user");

        mPostReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                arrayIndex.clear();
                for(DataSnapshot datasnapshot :snapshot.getChildren()){
                    num = datasnapshot.child("phoneNum").getValue(String.class);
//                    num = datasnapshot.getValue().toString();
                    arrayIndex.add(num);

                    Log.e("user phone",arrayIndex.get(arrayIndex.size()-1));
                    phone_num.setText(arrayIndex.get(arrayIndex.size()-1));
                    if(datasnapshot.child("companyNum").getValue(String.class).equals(getCompany_num))
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });




//               mPostReference.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//
////                arrayIndex.clear();
//                for(DataSnapshot datasnapshot :snapshot.getChildren()){
////                    num = datasnapshot.child("companyNum").getValue(String.class);
//                    num = datasnapshot.getValue().toString();
//                }
//            }

//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

        StartFirebaseLogin();

        phone_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneNumber = phone_num.getText().toString();
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        "+82"+phoneNumber.substring(1),        // OTP받을 사용자 핸드폰 번호
                        60,                 // Timeout duration
                        TimeUnit.SECONDS,   // Unit of timeout
                        Otp.this,               // Activity (for callback binding)
                        mCallbacks);

                Toast.makeText(Otp.this, phoneNumber,Toast.LENGTH_SHORT).show();
            }

        });


        otp_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                otpNumber = otp_num.getText().toString();
//                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode,otpNumber);
//                SignInWithPhone(credential);

                verifyCode(otpNumber);
            }
        });


    }

    private void SignInWithPhone(PhoneAuthCredential credential){
        auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(Otp.this, "correct OTP",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(Otp.this, "Incorrect OTP",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

//    private void sendVerificationCodeToUser(String phone_num) {
//        PhoneAuthProvider.getInstance().verifyPhoneNumber(
//                "+82"+phone_num,        // Phone number to verify
//                60,                 // Timeout duration
//                TimeUnit.SECONDS,   // Unit of timeout
//                TaskExecutors.MAIN_THREAD,               // Activity (for callback binding)
//                mCallbacks);        // OnVerificationStateChangedCallbacks
//    }

//    private  void verifyCode(String codeByUser){
//        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCodeBySystem,codeByUser);
//        signInTheUserByCredentials(credential);
//    }

//    private void signInTheUserByCredentials(PhoneAuthCredential credential){
//        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
//        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(Otp.this, new OnCompleteListener<AuthResult>() {
//            @Override
//            public void onComplete(@NonNull Task<AuthResult> task) {
//                if(task.isSuccessful()){
//                    Toast.makeText(Otp.this,"인증되었습니다.",Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent(Otp.this, Menu.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//
//                }
//                else{
//                    Toast.makeText(Otp.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }

//    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//
//        @Override
//        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
//            super.onCodeSent(s, forceResendingToken);
//            verificationCodeBySystem = s;
//        }
//
//        @Override
//        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
//
//            String code = phoneAuthCredential.getSmsCode();
//            if(code != null){
//                verifyCode(code);
//            }
//        }
//
//        @Override
//        public void onVerificationFailed(@NonNull FirebaseException e) {
//            Toast.makeText(Otp.this, "Error",Toast.LENGTH_SHORT).show();
//        }
//    };

    private void StartFirebaseLogin(){
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                Toast.makeText(Otp.this, "verification completed",Toast.LENGTH_SHORT).show();
            } //인증을 성공하였을 경우

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(Otp.this, "verification failed",Toast.LENGTH_SHORT).show();

            }//인증에 실패했을 경우
            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken){
                super.onCodeSent(s, forceResendingToken);
                verificationCode = s;
                Toast.makeText(Otp.this, "code sent",Toast.LENGTH_SHORT).show();
            }//서버로 OTP를 받을 사용자 번호 전송
        };
    }

    private void verifyCode(String code) {
        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode, code);
            SignInWithPhone(credential);
        }catch (Exception e){
            Toast toast = Toast.makeText(getApplicationContext(), "Verification Code is wrong, try again", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
        }

    }
}