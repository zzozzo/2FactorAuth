package com.example.twofactor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class Menu extends AppCompatActivity {

    String getCompany_num;
    static ArrayList<String> arrayRaspberry = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        Button button_check = (Button)findViewById(R.id.button1);
        Button button_otp = (Button)findViewById(R.id.button2);
        Button button_qr = (Button)findViewById(R.id.button3);

       getCompany_num = getIntent().getStringExtra("userID");

        button_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });



        button_otp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                arrayRaspberry.clear();
                db.collection("TwoFactor").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document : task.getResult()){
                                //index[0] = companyNum , [1] = log , [2] = result
                                arrayRaspberry.add(document.getData().get("company").toString());
                                arrayRaspberry.add(document.getData().get("log").toString());
                                arrayRaspberry.add(document.getData().get("result").toString());
//                                Log.d("TAG",document.getId() +"=>"+document.getData());
                                Log.e("Raspaberry",arrayRaspberry.get(arrayRaspberry.size()-1));

                                if(arrayRaspberry.get(arrayRaspberry.size()-1).equals("0")){
                                    Toast.makeText(getApplicationContext(),"1차인증 후 사용해주세요",Toast.LENGTH_SHORT).show();
                                }else{
                                    Intent intent = new Intent(getApplicationContext(),Otp.class);
                                    intent.putExtra("userID",getCompany_num);
                                    startActivity(intent);
                                }
                            }
                        }else{
                            Log.d("TAG","Error getting document :",task.getException());
                        }
                    }
                });




            }
        });

        button_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),QRcode.class);
                intent.putExtra("userID",getCompany_num);
                startActivity(intent);
            }
        });
    }
}