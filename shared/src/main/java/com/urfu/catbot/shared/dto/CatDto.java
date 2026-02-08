package com.urfu.catbot.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatDto {
  private Long id;
  private String name;
  private Integer ageMonths;
  private String color;
  private String breed;
  private String descriptionFa;
  private String descriptionEn;
  private String descriptionRu;
  private String photoFileId;
  private Long ownerId;
  private String ownerUsername;
  private LocalDate createdAt;
}
