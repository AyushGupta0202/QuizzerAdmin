package com.eggdevs.quizzeradmin;

import java.util.List;

public class CategoryModel {
   private String url, name;
   private List<String> sets;
   private String key;

   public CategoryModel() {
      ///for firebase
   }

   public CategoryModel(String name, List<String> sets, String url, String key) {
      this.url = url;
      this.name = name;
      this.sets = sets;
      this.key = key;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<String> getSets() {
      return sets;
   }

   public void setSets(List<String> sets) {
      this.sets = sets;
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }
}
