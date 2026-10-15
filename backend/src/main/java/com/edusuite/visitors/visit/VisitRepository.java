package com.edusuite.visitors.visit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

	List<Visit> findByVisitDateOrStateOrderByIdDesc(LocalDate visitDate, VisitState state);

	List<Visit> findTop50ByHostUsernameOrderByIdDesc(String username);

	List<Visit> findByStateAndCreatedAtBefore(VisitState state, Instant cutoff);

	Optional<Visit> findByPassNumberAndState(String passNumber, VisitState state);

	boolean existsByPassNumberAndState(String passNumber, VisitState state);

}
