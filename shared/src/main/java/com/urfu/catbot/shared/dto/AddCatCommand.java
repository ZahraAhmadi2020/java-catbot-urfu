package com.urfu.catbot.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCatCommand {
  private Long userId;
  private String username;
  private String photoFileId;
  private String name;
  private String color;
  private String breed;
  private Integer ageMonths;
  private String description;
}
