package com.eggdevs.quizzeradmin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.UUID;

public class AddQuestionActivity extends AppCompatActivity {

   private EditText tvQuestion;
   private RadioGroup options;
   private LinearLayout answers;
   private String categoryName;
   private int position;
   private Dialog loadingDialog;
   private QuestionModel questionModel;
   private String id;
   private String setId;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_add_question);

      Toolbar toolbar = findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      getSupportActionBar().setTitle("Add Question");
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      loadingDialog = new Dialog(this);
      loadingDialog.setContentView(R.layout.loading_dialog);
      loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
      loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
      loadingDialog.setCanceledOnTouchOutside(false);
      loadingDialog.setCancelable(false);

      tvQuestion = findViewById(R.id.tvQuestion);
      options = findViewById(R.id.options);
      answers = findViewById(R.id.answers);

      setId = getIntent().getStringExtra("setId");
      position = getIntent().getIntExtra("position", -1);
      categoryName = getIntent().getStringExtra("category");

      if (setId == null) {
         finish();
         return;
      }

      if (position != -1) {
         questionModel = QuestionsActivity.list.get(position);
         setData();
      }

      findViewById(R.id.btnUpload).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (tvQuestion.getText().toString().trim().isEmpty()) {
               tvQuestion.setError("Required");
               return;
            }
            uploadQuestion();
         }
      });
   }

   private void setData() {
      tvQuestion.setText(questionModel.getQuestion());

      ((EditText) answers.getChildAt(0)).setText(questionModel.getOptionA());
      ((EditText) answers.getChildAt(1)).setText(questionModel.getOptionB());
      ((EditText) answers.getChildAt(2)).setText(questionModel.getOptionC());
      ((EditText) answers.getChildAt(3)).setText(questionModel.getOptionD());

      for (int i = 0; i < answers.getChildCount(); i++) {
         if (((EditText) answers.getChildAt(i)).getText().toString().equals(questionModel.getCorrectAns())) {
            RadioButton radioButton = (RadioButton) options.getChildAt(i);
            radioButton.setChecked(true);
            break;
         }
      }
   }

   @Override
   public boolean onOptionsItemSelected(@NonNull MenuItem item) {
      if (item.getItemId() == android.R.id.home) {
         finish();
      }
      return super.onOptionsItemSelected(item);
   }

   private void uploadQuestion() {

      int correct = -1;
      for (int i = 0; i < options.getChildCount(); i++) {

         EditText etAnswer = (EditText) answers.getChildAt(i);
         if (etAnswer.getText().toString().trim().isEmpty()) {
            etAnswer.setError("Required");
            return;
         }

         RadioButton radioButton3 = (RadioButton) options.getChildAt(i);
         if (radioButton3.isChecked()) {
            correct = i;
            break;
         }
      }

      if (correct == -1) {
         Toast.makeText(this, "Please select the correct option.", Toast.LENGTH_SHORT).show();
         return;
      }

      final HashMap<String, Object> map = new HashMap<>();
      map.put("correctAns", ((EditText) answers.getChildAt(correct)).getText().toString().trim());
      map.put("optionA", ((EditText) answers.getChildAt(0)).getText().toString().trim());
      map.put("optionB", ((EditText) answers.getChildAt(1)).getText().toString().trim());
      map.put("optionC", ((EditText) answers.getChildAt(2)).getText().toString().trim());
      map.put("optionD", ((EditText) answers.getChildAt(3)).getText().toString().trim());
      map.put("question", tvQuestion.getText().toString().trim());
      map.put("setId", setId);

      if (position != -1) {
         id = questionModel.getId();
      } else {
         id = UUID.randomUUID().toString();
      }

      loadingDialog.show();
      FirebaseDatabase database = FirebaseDatabase.getInstance();
      database.getReference().child("SETS").child(setId).child(id).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
         @Override
         public void onComplete(@NonNull Task<Void> task) {
            if (task.isSuccessful()) {

               QuestionModel questionModel = new QuestionModel(id,
                       map.get("question").toString(),
                       map.get("optionA").toString(),
                       map.get("optionB").toString(),
                       map.get("optionC").toString(),
                       map.get("optionD").toString(),
                       map.get("correctAns").toString(),
                       map.get("setId").toString());

               if (position != -1) {
                  QuestionsActivity.list.set(position, questionModel);
               } else {
                  QuestionsActivity.list.add(questionModel);
               }

               finish();

            } else {
               Toast.makeText(AddQuestionActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
            loadingDialog.dismiss();
         }
      });
   }
}