package com.urfu.catbot.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCommentCommand {
  private Long catId;
  private Long userId;
  private String username;
  private String text;
}
