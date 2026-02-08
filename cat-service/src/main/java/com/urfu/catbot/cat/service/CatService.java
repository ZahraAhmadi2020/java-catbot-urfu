package com.urfu.catbot.cat.service;

import com.urfu.catbot.cat.domain.Cat;
import com.urfu.catbot.cat.domain.Comment;
import com.urfu.catbot.cat.domain.Reaction;
import com.urfu.catbot.cat.repository.CatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatService {
  private final CatRepository catRepository;

  @Transactional
  public Cat addCat(String name, String photoFileId, Long ownerId, String ownerUsername,
      String color, String breed, Integer ageMonths, String description) {

    Integer safeAge = (ageMonths != null && ageMonths > 0 && ageMonths <= 300) ? ageMonths : 12;

    Cat cat = new Cat(name, photoFileId, ownerId, ownerUsername, color, breed, safeAge, description);
    return catRepository.save(cat);
  }

  @Transactional
  public void addReaction(Long catId, Long userId, String username, String emoji) {
    try {
      Cat cat = catRepository.findById(catId)
          .orElseThrow(() -> new RuntimeException("Cat not found: " + catId));

      cat.getReactions().removeIf(r -> r.getUserId().equals(userId));
      cat.getReactions().add(new Reaction(cat, userId, username, emoji));
      catRepository.save(cat);
    } catch (Exception e) {
      System.err.println("⚠️ Warning: Failed to add reaction for cat " + catId + ": " + e.getMessage());

    }
  }

  // add comment
  @Transactional
  public void addComment(Long catId, Long userId, String username, String text) {
    try {

      Cat cat = catRepository.findById(catId)
          .orElseThrow(() -> new RuntimeException("Cat not found: " + catId));

      cat.getComments().add(new Comment(cat, userId, username, text));
      catRepository.save(cat);
    } catch (Exception e) {
      System.err.println("⚠️ Warning: Failed to add comment for cat " + catId + ": " + e.getMessage());

    }
  }

  @Transactional
  public void deleteCat(Long catId, Long userId) {
    Cat cat = catRepository.findById(catId)
        .orElseThrow(() -> new RuntimeException("Cat not found: " + catId));
    if (!cat.getOwnerId().equals(userId)) {
      throw new RuntimeException("Only owner can delete this cat");
    }
    catRepository.delete(cat);
  }

  public List<Cat> getAllCats() {
    return catRepository.findAllByOrderByCreatedAtDesc();
  }

  public List<Cat> getUserCats(Long userId) {
    return catRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
  }

  public Optional<Cat> getCatById(Long catId) {
    return catRepository.findById(catId);
  }
}
