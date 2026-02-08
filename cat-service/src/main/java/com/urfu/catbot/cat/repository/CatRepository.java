package com.urfu.catbot.cat.repository;

import com.urfu.catbot.cat.domain.Cat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatRepository extends JpaRepository<Cat, Long> {
  List<Cat> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

  List<Cat> findAllByOrderByCreatedAtDesc();
}
