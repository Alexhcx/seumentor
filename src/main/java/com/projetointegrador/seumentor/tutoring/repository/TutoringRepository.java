package com.projetointegrador.seumentor.tutoring.repository;

import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.user.model.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TutoringRepository extends JpaRepository<Tutoring, Long>, JpaSpecificationExecutor<Tutoring> {

    boolean existsByDisciplineId(Long disciplineId);

    boolean existsByMentorAndDisciplineAndTutoringDateAndStartTimeAndEndTimeAndStatusNotIn(
            User mentor,
            Discipline discipline,
            LocalDate tutoringDate,
            LocalTime startTime,
            LocalTime endTime,
            List<StatusTutoring> excludedStatuses 
    );

    @Query("SELECT t FROM Tutoring t LEFT JOIN t.topics tp " +
            "WHERE t.tutoringDate = :date " +
            "AND t.status IN :statusesToConsider " + 
            "AND ((t.mentor = :user) OR (tp.user = :user)) " +
            "AND (t.startTime < :endTime AND t.endTime > :startTime)") 
    List<Tutoring> findConflictingTutoringsForUserWithSpecificStatuses( 
            @Param("user") User user,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statusesToConsider") List<StatusTutoring> statusesToConsider 
    );
}
