package com.projetointegrador.seumentor.tutoring.model;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tutoring")
public class Tutoring extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_mentor", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User mentor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "discipline_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Discipline discipline;

  @Enumerated(EnumType.STRING)
  private TutoringClassType tutoringClassType;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private StatusTutoring status = StatusTutoring.PENDENTE;

  private LocalTime startTime;
  private LocalTime endTime;

  private LocalDate tutoringDate;

  private String local;
  private String linkVideo;

  private Integer maxParticipants;

  @Builder.Default
  private Boolean isChatEnable = false;

  @OneToOne(mappedBy = "tutoring", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private TutoringRating rating;

  @OneToMany(mappedBy = "tutoring", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  @ToString.Exclude
  private Set<TutoringParticipants> topics = new HashSet<>();

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    Tutoring tutoring = (Tutoring) o;
    return getId() != null && Objects.equals(getId(), tutoring.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
  }
}