package com.projetointegrador.seumentor.tutoring.model;

import java.io.Serializable;

import com.projetointegrador.seumentor.common.model.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false, exclude = {"tutoring"}) 
@ToString(callSuper = true, exclude = {"tutoring"}) 
@Table(name = "tutoring_rating")
public class TutoringRating extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutoring_id", unique = true, nullable = false) 
    private Tutoring tutoring;

    private Float mentorRating;
    private Float menteeRating;
    private String review;

}
