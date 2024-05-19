package com.eggdevs.quizzeradmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

   private List<CategoryModel> categoryModelList;
   private DeleteListener deleteListener;

   public CategoryAdapter(List<CategoryModel> categoryModelList, DeleteListener deleteListener) {
      this.categoryModelList = categoryModelList;
      this.deleteListener = deleteListener;
   }

   @NonNull
   @Override
   public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item, parent,
              false);
      return new ViewHolder(view);
   }

   @Override
   public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      holder.setData(categoryModelList.get(position).getUrl(),
              categoryModelList.get(position).getName(),
              categoryModelList.get(position).getKey(), position);
   }

   @Override
   public int getItemCount() {
      return categoryModelList.size();
   }

   public interface DeleteListener {
      void onDelete(String key, int position);
   }

   class ViewHolder extends RecyclerView.ViewHolder {

      private CircleImageView ivCategory;
      private TextView tvCategoryTitle;
      private ImageButton imgBtnDelete;

      public ViewHolder(@NonNull View itemView) {
         super(itemView);
         ivCategory = itemView.findViewById(R.id.ivCategory);
         tvCategoryTitle = itemView.findViewById(R.id.tvCategoryTitle);
         imgBtnDelete = itemView.findViewById(R.id.imgBtnDelete);
      }

      private void setData(String url, final String title, final String key,
                           final int position) {
         Glide.with(itemView.getContext()).load(url).into(ivCategory);
         tvCategoryTitle.setText(title);
         itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent setIntent = new Intent(itemView.getContext(), SetsActivity.class);
               setIntent.putExtra("title", title);
               setIntent.putExtra("position", position);
               setIntent.putExtra("key", key);
               itemView.getContext().startActivity(setIntent);
            }
         });

         imgBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               deleteListener.onDelete(key, position);
            }
         });
      }
   }
}
