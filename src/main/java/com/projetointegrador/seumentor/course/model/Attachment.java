package com.projetointegrador.seumentor.course.model;

import java.io.Serializable;

import com.projetointegrador.seumentor.chat.model.Conversations;
import com.projetointegrador.seumentor.common.model.BaseEntity;

import jakarta.persistence.Column; 
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn; 
import jakarta.persistence.ManyToOne;
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
@EqualsAndHashCode(callSuper = true, exclude = {"conversation"})
@ToString(callSuper = true, exclude = {"conversation"})
public class Attachment extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false) 
    private String fileName; 

    private String fileType;

    @Column(nullable = false) 
    private String filePath; 

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "conversation_id", nullable = false) 
    private Conversations conversation;
}