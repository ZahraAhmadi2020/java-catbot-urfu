package com.urfu.catbot.cat.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cats")
@Data
@NoArgsConstructor
public class Cat {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String photoFileId;

  @Column(nullable = false)
  private Long ownerId;

  @Column(nullable = false)
  private String ownerUsername;

  @Column(nullable = false)
  private String color;

  @Column(nullable = false)
  private String breed;

  @Column(nullable = false)
  private Integer age;

  @Column(columnDefinition = "TEXT")
  private String description;

  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "cat", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Reaction> reactions = new ArrayList<>();

  @OneToMany(mappedBy = "cat", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Comment> comments = new ArrayList<>();

  public Cat(String name, String photoFileId, Long ownerId, String ownerUsername,
      String color, String breed, Integer age, String description) {
    this.name = name;
    this.photoFileId = photoFileId;
    this.ownerId = ownerId;
    this.ownerUsername = ownerUsername;
    this.color = color;
    this.breed = breed;
    this.age = age;
    this.description = description;
  }
}
