package com.swipehire.web.repository;

import com.swipehire.web.entity.SkillSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillSynonymRepository extends JpaRepository<SkillSynonym, String> {
    List<SkillSynonym> findAllByOrderByAliasAsc();
}
