package com.projetointegrador.seumentor.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.chat.model.Conversations;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationsRepository extends JpaRepository<Conversations, Integer> {

    List<Conversations> findByTutoringIdOrderByCreatedAtAsc(Long tutoringId);

}
