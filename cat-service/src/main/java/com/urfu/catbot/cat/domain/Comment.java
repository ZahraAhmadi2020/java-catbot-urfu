package com.urfu.catbot.cat.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
public class Comment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cat_id", nullable = false)
  private Cat cat;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private String username;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String text;

  private LocalDateTime commentedAt = LocalDateTime.now();

  public Comment(Cat cat, Long userId, String username, String text) {
    this.cat = cat;
    this.userId = userId;
    this.username = username;
    this.text = text;
  }
}
