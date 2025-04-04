package com.projetointegrador.seumentor.tutoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.tutoring.model.UserRating;

public interface UserRatingRepository extends JpaRepository<UserRating, Integer> {

}
