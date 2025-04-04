package com.projetointegrador.seumentor.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.chat.model.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {

}
