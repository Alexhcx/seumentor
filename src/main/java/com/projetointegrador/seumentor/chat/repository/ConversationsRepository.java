package com.projetointegrador.seumentor.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.chat.model.Conversations;

public interface ConversationsRepository extends JpaRepository<Conversations, Integer> {

}
