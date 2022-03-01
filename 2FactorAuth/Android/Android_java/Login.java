package com.example.twofactor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Login extends AppCompatActivity {

    EditText company_num;
    TextView join;
    Button toLogin;

    String num;
    static  ArrayList<String> arrayIndex = new ArrayList<String>();

    private DatabaseReference mPostReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        join = (TextView)findViewById(R.id.join);
        toLogin= (Button)findViewById(R.id.toLogin);
        company_num = (EditText)findViewById(R.id.company_num);

//        mPostReference = FirebaseDatabase.getInstance().getReference("user");
        mPostReference = FirebaseDatabase.getInstance().getReference("user");

        mPostReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                arrayIndex.clear();
                for(DataSnapshot datasnapshot :snapshot.getChildren()){
                    num = datasnapshot.child("companyNum").getValue(String.class);
//                    num = datasnapshot.getValue().toString();
                    arrayIndex.add(num);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        toLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                if(!IsExistNum()){
                    Toast.makeText(Login.this, "등록된 사용자가 아닙니다.",Toast.LENGTH_SHORT).show();;
                }else{
                    Intent intent = new Intent(getApplicationContext(),Menu.class);
                    intent.putExtra("userID",company_num.getText().toString());
                    startActivity(intent);
                }

            }
        });

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
            }
        });




    }

    public boolean IsExistNum(){
        boolean IsExist = arrayIndex.contains(company_num.getText().toString());
        return IsExist;
    }


}