package com.projetointegrador.seumentor.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.chat.model.Attachment;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {

}
