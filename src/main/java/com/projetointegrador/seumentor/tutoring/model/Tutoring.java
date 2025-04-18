package com.projetointegrador.seumentor.tutoring.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType; // Pode ser necessário importar
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany; // Importar OneToMany
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
@EqualsAndHashCode(callSuper = false, exclude = { "mentor", "mentee", "discipline", "rating", "topics" })
@ToString(callSuper = true, exclude = { "mentor", "mentee", "discipline", "rating", "topics" })
@Table(name = "tutoring")
public class Tutoring extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  private LocalDateTime startTime;
  private LocalDateTime endTime;

  private String local;
  private String linkVideo;

  private Integer maxParticipants;

  @Builder.Default
  private Boolean isChatEnable = false;

  @OneToOne(mappedBy = "tutoring", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private TutoringRating rating;

  @OneToMany(mappedBy = "tutoring", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<TutoringTopics> topics = new HashSet<>();

}