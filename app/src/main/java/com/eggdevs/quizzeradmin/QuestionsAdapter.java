package com.eggdevs.quizzeradmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.ViewHolder> {

   private List<QuestionModel> list;
   private String categoryName;
   private DeleteListener deleteListener;

   public QuestionsAdapter(List<QuestionModel> list, String categoryName, DeleteListener deleteListener) {
      this.list = list;
      this.categoryName = categoryName;
      this.deleteListener = deleteListener;
   }

   @NonNull
   @Override
   public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_item, parent, false);
      return new ViewHolder(view);
   }

   @Override
   public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

      String question = list.get(position).getQuestion();
      String answer = list.get(position).getCorrectAns();
      holder.setData(question, answer, position);
   }

   @Override
   public int getItemCount() {
      return list.size();
   }

   public interface DeleteListener {
      void onLongClick(int position, String id);
   }

   public class ViewHolder extends RecyclerView.ViewHolder {

      private TextView tvQuestion, tvAnswer;

      public ViewHolder(@NonNull View itemView) {
         super(itemView);

         tvQuestion = itemView.findViewById(R.id.tvQuestion);
         tvAnswer = itemView.findViewById(R.id.tvAnswer);
      }

      private void setData(String question, String answer, final int position) {
         tvQuestion.setText(position + 1 + ". " + question);
         tvAnswer.setText("Ans. " + answer);

         itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               itemView.getContext().startActivity(new Intent(itemView.getContext(), AddQuestionActivity.class)
                       .putExtra("category", categoryName)
                       .putExtra("setId", list.get(position).getSet())
                       .putExtra("position", position));
            }
         });

         itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
               deleteListener.onLongClick(position, list.get(position).getId());
               return false;
            }
         });
      }
   }
}
