package com.projetointegrador.seumentor.tutoring.repository;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;
import com.projetointegrador.seumentor.user.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

        List<MentorAvailability> findByUserId(Long userId);

        List<MentorAvailability> findByDisciplineId(Long disciplineId);

        List<MentorAvailability> findByUserIdAndDisciplineId(Long userId, Long disciplineId);

        boolean existsByDisciplineId(Long disciplineId);

        List<MentorAvailability> findByUserIdIn(List<Long> userIds);

        List<MentorAvailability> findByUserIdAndDayOfWeek(Long userId, DayWeek dayOfWeek);

        List<MentorAvailability> findByDayOfWeek(DayWeek dayOfWeek);

        // NOVO MÉTODO: Para verificar se existe uma disponibilidade ativa que cobre o
        // horário da mentoria
        @Query("SELECT ma FROM MentorAvailability ma " +
                        "WHERE ma.user = :mentor " +
                        "AND ma.discipline = :discipline " +
                        "AND ma.dayOfWeek = :dayOfWeek " +
                        "AND ma.isAvailable = true " +
                        "AND ma.startTime <= :tutoringStartTime " +
                        "AND ma.endTime >= :tutoringEndTime")
        Optional<MentorAvailability> findCoveringAndActiveAvailability(
                        @Param("mentor") User mentor,
                        @Param("discipline") Discipline discipline,
                        @Param("dayOfWeek") DayWeek dayOfWeek,
                        @Param("tutoringStartTime") LocalTime tutoringStartTime,
                        @Param("tutoringEndTime") LocalTime tutoringEndTime);
}
