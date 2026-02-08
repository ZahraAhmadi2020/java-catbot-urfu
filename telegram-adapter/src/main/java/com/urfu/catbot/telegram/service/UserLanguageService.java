package com.urfu.catbot.telegram.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserLanguageService {
  private final Map<Long, String> userLanguages = new HashMap<>();
  private final Map<Long, String> userStates = new HashMap<>();
  private final Map<Long, CatData> userCatData = new HashMap<>();

  public void setLanguage(Long userId, String languageCode) {
    userLanguages.put(userId, languageCode);
  }

  public String getLanguage(Long userId) {
    return userLanguages.getOrDefault(userId, "fa");
  }

  public boolean hasSelectedLanguage(Long userId) {
    return userLanguages.containsKey(userId);
  }

  public void setState(Long userId, String state) {
    userStates.put(userId, state);
  }

  public String getState(Long userId) {
    return userStates.getOrDefault(userId, "IDLE");
  }

  public void setCatData(Long userId, CatData catData) {
    userCatData.put(userId, catData);
  }

  public CatData getCatData(Long userId) {
    return userCatData.get(userId);
  }

  public void clearUserData(Long userId) {
    userStates.remove(userId);
    userCatData.remove(userId);
  }

  public static class CatData {
    private String photoFileId;
    private String name;
    private String color;
    private String breed;
    private Integer age;
    private String description;

    public String getPhotoFileId() {
      return photoFileId;
    }

    public void setPhotoFileId(String photoFileId) {
      this.photoFileId = photoFileId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    public String getBreed() {
      return breed;
    }

    public void setBreed(String breed) {
      this.breed = breed;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
