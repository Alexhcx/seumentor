package com.projetointegrador.seumentor.course.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.user.model.UserDisciplinePreference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Table(name = "discipline")
public class Discipline extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue
  private Integer id;
  private String disciplineName;
  private String description;

  @OneToMany(mappedBy = "discipline", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Tutoring> relatedTutorings = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_area_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private CourseArea courseArea;

  @OneToMany(mappedBy = "discipline", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<UserDisciplinePreference> userPreferences = new HashSet<>();

}
