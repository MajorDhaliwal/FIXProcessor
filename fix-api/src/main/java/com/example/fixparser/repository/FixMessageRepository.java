package com.example.fixparser.repository;

import com.example.fixparser.model.FixMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FixMessageRepository extends JpaRepository<FixMessageEntity, Long> {
}
