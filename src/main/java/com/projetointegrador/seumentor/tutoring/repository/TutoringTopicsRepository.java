package com.projetointegrador.seumentor.tutoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projetointegrador.seumentor.tutoring.model.TutoringTopics;

@Repository
public interface TutoringTopicsRepository extends JpaRepository<TutoringTopics, Long>  {

}
