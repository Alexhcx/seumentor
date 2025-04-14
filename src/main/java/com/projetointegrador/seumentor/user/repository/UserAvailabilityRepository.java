package com.projetointegrador.seumentor.user.repository;

import com.projetointegrador.seumentor.user.model.UserAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAvailabilityRepository extends JpaRepository<UserAvailability, Long> {

    List<UserAvailability> findByUserId(Long userId);

    List<UserAvailability> findByDisciplineId(Long disciplineId);

    List<UserAvailability> findByUserIdAndDisciplineId(Long userId, Long disciplineId);

    boolean existsByDisciplineId(Long disciplineId);

}
