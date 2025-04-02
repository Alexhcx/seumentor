package com.projetointegrador.seumentor.course.model;

import com.projetointegrador.seumentor.user.model.User; 

import jakarta.persistence.Column; 
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType; 
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn; 
import jakarta.persistence.ManyToOne; 
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint; 
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
@Table(name = "discipline_preference", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "discipline_id", "user_id" }, name = "uk_discipline_user_preference")
})
@EqualsAndHashCode(exclude = { "discipline", "user" })
@ToString(exclude = { "discipline", "user" })
public class DisciplinePreference {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false)
  private Integer vote;

  @ManyToOne(fetch = FetchType.LAZY, optional = false) 
  @JoinColumn(name = "discipline_id", nullable = false) 
  private Discipline discipline;
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false) 
  private User user; 
}