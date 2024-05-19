package com.eggdevs.quizzeradmin;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class QuestionsActivity extends AppCompatActivity {

   public static List<QuestionModel> list;
   FirebaseDatabase database = FirebaseDatabase.getInstance();
   DatabaseReference myRef = database.getReference();
   private QuestionsAdapter adapter;
   private Dialog loadingDialog;
   private String categoryName;
   private String setId;
   private TextView tvLoadingText;

   public static final int CELL_COUNT = 6;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_quesitons);

      Toolbar toolbar = findViewById(R.id.toolbar);
      RecyclerView recyclerView = findViewById(R.id.questionsRecyclerView);

      loadingDialog = new Dialog(this);
      loadingDialog.setContentView(R.layout.loading_dialog);
      loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
      loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
      loadingDialog.setCanceledOnTouchOutside(false);
      loadingDialog.setCancelable(false);
      tvLoadingText = loadingDialog.findViewById(R.id.tvLoadingText);

      categoryName = getIntent().getStringExtra("category");
      setId = getIntent().getStringExtra("setId");

      setSupportActionBar(toolbar);
      getSupportActionBar().setTitle(categoryName);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      LinearLayoutManager layoutManager = new LinearLayoutManager(this);
      layoutManager.setOrientation(RecyclerView.VERTICAL);
      recyclerView.setLayoutManager(layoutManager);

      list = new ArrayList<>();

      adapter = new QuestionsAdapter(list, categoryName,
              new QuestionsAdapter.DeleteListener() {
         @Override
         public void onLongClick(final int position, final String id) {
            new AlertDialog.Builder(QuestionsActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                    .setTitle("Delete question")
                    .setMessage("Are you sure to delete this question?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                          loadingDialog.show();
                          myRef.child("SETS").child(setId).child(id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                             @Override
                             public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                   list.remove(position);
                                   adapter.notifyItemRemoved(position);
                                } else {
                                   Toast.makeText(QuestionsActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                                }
                                loadingDialog.dismiss();
                             }
                          });
                       }
                    })
                    .setNegativeButton("No", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
         }
      });
      recyclerView.setAdapter(adapter);

      getData(categoryName, setId);

      findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivity(new Intent(QuestionsActivity.this, AddQuestionActivity.class)
                    .putExtra("category", categoryName)
                    .putExtra("setId", setId));
         }
      });

      findViewById(R.id.btnExcel).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (ActivityCompat.checkSelfPermission(QuestionsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
               selectFile();
            } else {
               ActivityCompat.requestPermissions(QuestionsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
         }
      });
   }

   private void selectFile() {

      startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), "Select File"),
              101);
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      if (requestCode == 101) {
         if (resultCode == RESULT_OK) {

            String filePath = data.getData().getPath();
            if (filePath.endsWith("xlsx")) {
               readFile(data.getData());
            } else {
               Toast.makeText(this, "choose an excel file", Toast.LENGTH_SHORT).show();
            }
         }
      }
   }

   private void readFile(final Uri fileUri) {

      tvLoadingText.setText("Scanning Questions...");
      loadingDialog.show();

      AsyncTask.execute(new Runnable() {
         @Override
         public void run() {
            final HashMap<String, Object> parentMap = new HashMap<>();
            final List<QuestionModel> tempList = new ArrayList<>();

            try {
               InputStream inputStream = getContentResolver().openInputStream(fileUri);
               XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
               XSSFSheet sheet = workbook.getSheetAt(0);
               FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

               int rowsCount = sheet.getPhysicalNumberOfRows();

               if (rowsCount > 0) {
                  for (int r = 0; r < rowsCount; r++) {
                     Row row = sheet.getRow(r);

                     if (row.getPhysicalNumberOfCells() == CELL_COUNT) {
                        String question = getCellData(row, 0, formulaEvaluator);
                        String optionA = getCellData(row, 1, formulaEvaluator);
                        String optionB = getCellData(row, 2, formulaEvaluator);
                        String optionC = getCellData(row, 3, formulaEvaluator);
                        String optionD = getCellData(row, 4, formulaEvaluator);
                        String correctAns = getCellData(row, 5, formulaEvaluator);

                        if (correctAns.equals(optionA) || correctAns.equals(optionB) || correctAns.equals(optionC) || correctAns.equals(optionD)) {
                           HashMap<String, Object> questionMap = new HashMap<>();

                           questionMap.put("question", question);
                           questionMap.put("optionA", optionA);
                           questionMap.put("optionB", optionB);
                           questionMap.put("optionC", optionC);
                           questionMap.put("optionD", optionD);
                           questionMap.put("correctAns", correctAns);
                           questionMap.put("setId", setId);

                           String id = UUID.randomUUID().toString();

                           parentMap.put(id, questionMap);

                           tempList.add(new QuestionModel(id, question, optionA, optionB, optionC, optionD, correctAns, setId));

                        } else {

                           final int finalR = r;
                           runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                 tvLoadingText.setText("Loading...");
                                 loadingDialog.dismiss();
                                 Toast.makeText(QuestionsActivity.this, "Row number " + (finalR + 1) + " has no correct option", Toast.LENGTH_SHORT).show();

                              }
                           });
                           return;
                        }

                     } else {

                        final int finalR1 = r;
                        runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                              tvLoadingText.setText("Loading...");
                              loadingDialog.dismiss();
                              Toast.makeText(QuestionsActivity.this, "Row number " + (finalR1 + 1) + " has incorrect data", Toast.LENGTH_SHORT).show();

                           }
                        });
                        return;
                     }

                     runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                           tvLoadingText.setText("Uploading...");
                           FirebaseDatabase database = FirebaseDatabase.getInstance();
                           database.getReference().child("SETS").child(setId).updateChildren(parentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                              @Override
                              public void onComplete(@NonNull Task<Void> task) {
                                 if (task.isSuccessful()) {
                                    list.addAll(tempList);
                                    adapter.notifyDataSetChanged();
                                 } else {
                                    tvLoadingText.setText("Loading...");
                                    Toast.makeText(QuestionsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                 }
                                 loadingDialog.dismiss();
                              }
                           });
                        }
                     });


                  }
               } else {

                  runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                        tvLoadingText.setText("Loading...");
                        loadingDialog.dismiss();
                        Toast.makeText(QuestionsActivity.this, "File is empty", Toast.LENGTH_SHORT).show();

                     }
                  });
                  return;
               }
            }
            catch (final IOException e) {
               e.printStackTrace();
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     tvLoadingText.setText("Loading...");
                     loadingDialog.dismiss();
                     Toast.makeText(QuestionsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                  }
               });
            }
         }
      });
   }

   private String getCellData(Row row, int cellPosition, FormulaEvaluator formulaEvaluator) {
      String value = "";
      Cell cell = row.getCell(cellPosition);

      switch (cell.getCellType()) {
         case Cell.CELL_TYPE_BOOLEAN:
            return value + cell.getBooleanCellValue();
         case Cell.CELL_TYPE_NUMERIC:
            return value + cell.getNumericCellValue();
         case Cell.CELL_TYPE_STRING:
            return value + cell.getStringCellValue();
         default:
            return value;
      }
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);

      if (requestCode == 101) {
         if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectFile();
         } else {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
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

   private void getData(String categoryName, final String setId) {
      loadingDialog.show();
      myRef.child("SETS").child(setId).addListenerForSingleValueEvent(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            for (DataSnapshot snapshot1 : snapshot.getChildren()) {

               String id = snapshot1.getKey();
               String question = snapshot1.child("question").getValue().toString();
               String optionA = snapshot1.child("optionA").getValue().toString();
               String optionB = snapshot1.child("optionB").getValue().toString();
               String optionC = snapshot1.child("optionC").getValue().toString();
               String optionD = snapshot1.child("optionD").getValue().toString();
               String correctAns = snapshot1.child("correctAns").getValue().toString();

               list.add(new QuestionModel(id, question, optionA, optionB, optionC, optionD, correctAns, setId));
            }
            loadingDialog.dismiss();
            adapter.notifyDataSetChanged();
         }

         @Override
         public void onCancelled(@NonNull DatabaseError error) {

            Toast.makeText(QuestionsActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            finish();
         }
      });
   }

   @Override
   protected void onStart() {
      super.onStart();
      adapter.notifyDataSetChanged();
   }
}