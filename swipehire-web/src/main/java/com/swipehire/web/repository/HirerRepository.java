package com.swipehire.web.repository;

import com.swipehire.web.entity.Hirer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HirerRepository extends JpaRepository<Hirer, Long> {
}
