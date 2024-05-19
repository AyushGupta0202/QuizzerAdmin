package com.eggdevs.quizzeradmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;


public class GridAdapter extends BaseAdapter {

   public List<String> sets;
   private String category;
   private GridListener gridListener;

   public GridAdapter(List<String> sets, String category, GridListener gridListener) {
      this.sets = sets;
      this.category = category;
      this.gridListener = gridListener;
   }

   @Override
   public int getCount() {
      return sets.size() + 1;
   }

   @Override
   public Object getItem(int i) {
      return null;
   }

   @Override
   public long getItemId(int i) {
      return 0;
   }

   @Override
   public View getView(final int position, View view, final ViewGroup viewGroup) {
      View v;
      if (view == null) {
         v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.set_item, viewGroup, false);
      } else {
         v = view;
      }

      if (position == 0) {
         ((TextView) v.findViewById(R.id.tvSet)).setText("+");
      } else {
         ((TextView) v.findViewById(R.id.tvSet)).setText(String.valueOf(position));
      }
      v.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {

            if (position == 0) {
               //add
               gridListener.addSet();
            } else {
               viewGroup.getContext().startActivity(new Intent(viewGroup.getContext(), QuestionsActivity.class)
                       .putExtra("category", category)
                       .putExtra("setId", sets.get(position - 1)));
            }
         }
      });

      v.setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View view) {

            if (position != 0) {
               gridListener.onLongClick(sets.get(position - 1), position);
            }
            return false;

         }
      });

      return v;
   }

   public interface GridListener {
      void addSet();
      void onLongClick(String setId, int position);
   }
}
