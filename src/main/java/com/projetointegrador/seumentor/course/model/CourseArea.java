package com.projetointegrador.seumentor.course.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.projetointegrador.seumentor.common.model.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "course_area")
public class CourseArea extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(unique = true)
  private String course;
  private String area;

  @OneToMany(mappedBy = "courseArea", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<Discipline> disciplines = new HashSet<>();

}
