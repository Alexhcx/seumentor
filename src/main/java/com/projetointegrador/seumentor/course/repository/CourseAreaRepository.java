package com.projetointegrador.seumentor.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projetointegrador.seumentor.course.model.CourseArea;

@Repository
public interface CourseAreaRepository extends JpaRepository<CourseArea, Long> {

  boolean existsByCourseAndArea(String course,
      String area);

}
