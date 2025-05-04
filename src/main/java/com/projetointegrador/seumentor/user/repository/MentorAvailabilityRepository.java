package com.projetointegrador.seumentor.user.repository;

import com.projetointegrador.seumentor.user.model.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

    List<MentorAvailability> findByUserId(Long userId);

    List<MentorAvailability> findByDisciplineId(Long disciplineId);

    List<MentorAvailability> findByUserIdAndDisciplineId(Long userId, Long disciplineId);

    boolean existsByDisciplineId(Long disciplineId);

    List<MentorAvailability> findByUserIdIn(List<Long> userIds);

}
