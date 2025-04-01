package com.projetointegrador.seumentor.user.model;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.course.model.Discipline;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Table(name = "user_discipline_preference", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_id", "discipline_id" })
})
public class UserDisciplinePreference extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "discipline_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Discipline discipline;

  @Builder.Default
  @Column(nullable = false)
  private Boolean isMentor = false;

}
