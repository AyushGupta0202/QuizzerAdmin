package com.eggdevs.quizzeradmin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryActivity extends AppCompatActivity {

   public static List<CategoryModel> list;

   FirebaseDatabase database = FirebaseDatabase.getInstance();
   DatabaseReference myRef = database.getReference();
   private Dialog loadingDialog, categoryDialog;

   private CircleImageView civCategoryImage;
   private EditText etCategoryName;
   private Uri imageUri;
   private String downloadUrl;
   private CategoryAdapter adapter;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_category);
      Toolbar toolbar = findViewById(R.id.toolbar);
      RecyclerView recyclerView = findViewById(R.id.categoriesRecyclerView);

      setSupportActionBar(toolbar);
      getSupportActionBar().setTitle("Categories");
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      loadingDialog = new Dialog(this);
      loadingDialog.setContentView(R.layout.loading_dialog);
      loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
      loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
      loadingDialog.setCanceledOnTouchOutside(false);
      loadingDialog.setCancelable(false);

      setCategoryDialog();

      LinearLayoutManager layoutManager = new LinearLayoutManager(this);
      layoutManager.setOrientation(RecyclerView.VERTICAL);

      recyclerView.setLayoutManager(layoutManager);

      list = new ArrayList<>();

      adapter = new CategoryAdapter(list, new CategoryAdapter.DeleteListener() {
         @Override
         public void onDelete(final String key, final int position) {

            new AlertDialog.Builder(CategoryActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                    .setTitle("Delete category")
                    .setMessage("Are you sure to delete this category?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                          loadingDialog.show();
                          myRef.child("Categories").child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                             @Override
                             public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                   for (String setIds : list.get(position).getSets()) {
                                      myRef.child("SETS").child(setIds).removeValue();
                                   }
                                   list.remove(position);
                                   adapter.notifyDataSetChanged();

                                } else {
                                   Toast.makeText(CategoryActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
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

      loadingDialog.show();
      myRef.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
         @Override
         public void onDataChange(@NonNull DataSnapshot snapshot) {
            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

               List<String> sets = new ArrayList<>();

               for (DataSnapshot dataSnapshot1 : dataSnapshot.child("sets").getChildren()) {
                  sets.add(dataSnapshot1.getKey());
               }
               
               list.add(new CategoryModel(dataSnapshot.child("name").getValue().toString(),
                       sets,
                       dataSnapshot.child("url").getValue().toString(),
                       dataSnapshot.getKey()));
            }
            adapter.notifyDataSetChanged();
            loadingDialog.dismiss();
         }

         @Override
         public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(CategoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            finish();
         }
      });
   }

   private void setCategoryDialog() {
      categoryDialog = new Dialog(this);
      categoryDialog.setContentView(R.layout.add_category_dialog);
      categoryDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
      categoryDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,
              LinearLayout.LayoutParams.WRAP_CONTENT);
      categoryDialog.setCanceledOnTouchOutside(true);
      categoryDialog.setCancelable(true);

      civCategoryImage = categoryDialog.findViewById(R.id.category_image);
      etCategoryName = categoryDialog.findViewById(R.id.etCategoryName);

      civCategoryImage.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            startActivityForResult(new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 101);
         }
      });

      categoryDialog.findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (etCategoryName.getText().toString().isEmpty()) {
               etCategoryName.setError("Required");
               return;
            }

            for (CategoryModel model : list) {
               if (etCategoryName.getText().toString().equals(model.getName())) {
                  etCategoryName.setError("Category exists!");
                  return;
               }
            }

            if (imageUri == null) {
               Toast.makeText(CategoryActivity.this, "Please select an image", Toast.LENGTH_SHORT).show();
               return;
            }
            categoryDialog.dismiss();
            /// upload data

            uploadData();
         }
      });
   }

   private void uploadData() {
      loadingDialog.show();

      StorageReference storageReference = FirebaseStorage.getInstance().getReference();

      final StorageReference imageReference = storageReference.child("categories").child(imageUri.getLastPathSegment());

      UploadTask uploadTask = imageReference.putFile(imageUri);

      Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
         @Override
         public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
            if (!task.isSuccessful()) {
               throw task.getException();
            }

            // Continue with the task to get the download URL
            return imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
               @Override
               public void onComplete(@NonNull Task<Uri> task) {
                  if (task.isSuccessful()) {
                     downloadUrl = task.getResult().toString();
                     uploadCategoryName();
                  } else {
                     loadingDialog.dismiss();
                     Toast.makeText(CategoryActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                  }
               }
            });
         }
      }).addOnCompleteListener(new OnCompleteListener<Uri>() {
         @Override
         public void onComplete(@NonNull Task<Uri> task) {
            if (task.isSuccessful()) {
               Uri downloadUri = task.getResult();
            } else {
               // Handle failures
               // ...
               loadingDialog.dismiss();
               Toast.makeText(CategoryActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
         }
      });
   }

   private void uploadCategoryName() {
      Map<String, Object> map = new HashMap<>();
      map.put("name", etCategoryName.getText().toString().trim());
      map.put("sets", 0);
      map.put("url", downloadUrl);

      final String id = UUID.randomUUID().toString();

      FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

      firebaseDatabase.getReference().child("Categories").child(id).setValue(map)
              .addOnCompleteListener(new OnCompleteListener<Void>() {
                 @Override
                 public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                       list.add(new CategoryModel(etCategoryName.getText().toString().trim(), new ArrayList<String>(), downloadUrl,
                               id));
                       adapter.notifyDataSetChanged();
                    } else {
                       Toast.makeText(CategoryActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                    loadingDialog.dismiss();
                 }
              });
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.menu, menu);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(@NonNull MenuItem item) {
      if (item.getItemId() == R.id.action_add) {
         /// dialog show
         categoryDialog.show();
      }

      if (item.getItemId() == R.id.action_log_out) {
         new AlertDialog.Builder(CategoryActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                 .setTitle("Log out?")
                 .setMessage("Are you sure want to log out?")
                 .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                       loadingDialog.show();
                       FirebaseAuth.getInstance().signOut();
                       startActivity(new Intent(CategoryActivity.this, MainActivity.class));
                       finish();
                    }
                 })
                 .setNegativeButton("No", null)
                 .setIcon(android.R.drawable.ic_dialog_alert)
                 .show();
      }
      return super.onOptionsItemSelected(item);
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == 101) {

         if (resultCode == RESULT_OK) {
            imageUri = data.getData();
            civCategoryImage.setImageURI(imageUri);
         }
      }
   }
}