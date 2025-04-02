package com.projetointegrador.seumentor.tutoring.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@EqualsAndHashCode(callSuper = false, exclude = { "mentor", "mentee", "discipline", "rating" })
@ToString(callSuper = true, exclude = { "mentor", "mentee", "discipline", "rating" })
@Table(name = "tutoring")
public class Tutoring extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_mentor", nullable = false)
  private User mentor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_mentee", nullable = false)
  private User mentee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "discipline_id", nullable = false)
  private Discipline discipline;

  @Enumerated(EnumType.STRING)
  private ClassType classType;

  @Enumerated(EnumType.STRING)
  private StatusTutoring status;
  private LocalDateTime time;
  private String local;
  private String linkVideo;

  @Builder.Default
  private Boolean isChatEnable = false;

  @OneToOne(mappedBy = "tutoring", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private TutoringRating rating;

}
