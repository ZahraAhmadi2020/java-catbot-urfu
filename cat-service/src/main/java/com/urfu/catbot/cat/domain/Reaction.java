package com.urfu.catbot.cat.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reactions")
@Data
@NoArgsConstructor
public class Reaction {
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

  @Column(nullable = false)
  private String emoji;

  private LocalDateTime reactedAt = LocalDateTime.now();

  public Reaction(Cat cat, Long userId, String username, String emoji) {
    this.cat = cat;
    this.userId = userId;
    this.username = username;
    this.emoji = emoji;
  }
}
