package com.uniwiki.repository;

import com.uniwiki.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

// Question 엔티티의 기본 CRUD 기능을 제공
public interface QuestionRepository extends JpaRepository<Question, Long> {
}