package com.projetointegrador.seumentor.user.model;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.course.model.Discipline; // <-- Import Discipline
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false, exclude = {"user", "discipline"}) 
@ToString(callSuper = true, exclude = {"user", "discipline"}) 
@Table(name = "user_availability", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "discipline_id", "day_of_week", "start_time", "end_time"}) 
})
public class UserAvailability extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false) 
    private Discipline discipline; 

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek; 

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; 

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; 
}
