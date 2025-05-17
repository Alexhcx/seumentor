package com.projetointegrador.seumentor.tutoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutoringParticipantsRepository extends JpaRepository<TutoringParticipants, Long> {

    List<TutoringParticipants> findByUserId(Long userId);

    int countByTutoringId(Long tutoringId);

    boolean existsByTutoringIdAndUserId(Long tutoringId, Long id);

    @Transactional
    void deleteAllByTutoringId(Long tutoringId);

    Optional<TutoringParticipants> findByTutoringIdAndUserId(Long tutoringId, Long userId);
}
