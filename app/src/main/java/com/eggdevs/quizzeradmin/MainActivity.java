package com.eggdevs.quizzeradmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
   private TextView etEmail, etPassword;
   private FirebaseAuth firebaseAuth;
   private ProgressBar progressBar;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      etEmail = findViewById(R.id.etEmail);
      etPassword = findViewById(R.id.etPassword);
      progressBar = findViewById(R.id.progressBar);

      firebaseAuth = FirebaseAuth.getInstance();

      if (firebaseAuth.getCurrentUser() != null) {
         /// category intent
         startActivity(new Intent(this, CategoryActivity.class));
         finish();
         return;
      }

      findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {

            if (etEmail.getText().toString().isEmpty()) {
               etEmail.setError("Required");
               return;
            } else {
               etEmail.setError(null);
            }

            if (etPassword.getText().toString().isEmpty()) {
               etPassword.setError("Required");
               return;
            } else {
               etPassword.setError(null);
            }

            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth.signInWithEmailAndPassword(etEmail.getText().toString().trim(),
                    etPassword.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
               @Override
               public void onComplete(@NonNull Task<AuthResult> task) {
                  if (task.isSuccessful()) {
                     startActivity(new Intent(MainActivity.this, CategoryActivity.class));
                     finish();
                  } else {
                     Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_SHORT).show();
                  }
                  progressBar.setVisibility(View.INVISIBLE);
               }
            });

         }
      });
   }
}