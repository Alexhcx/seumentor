package com.projetointegrador.seumentor.chat.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.course.model.Attachment;
import com.projetointegrador.seumentor.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@EqualsAndHashCode(callSuper = true, exclude = { "sender", "receiver", "attachments" })
@ToString(callSuper = true, exclude = { "sender", "receiver", "attachments" })
public class Conversations extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  private String chatMessage;

  @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Attachment> attachments = new HashSet<>();

  public void addAttachment(Attachment attachment) {
    attachments.add(attachment);
    attachment.setConversation(this);
  }

  public void removeAttachment(Attachment attachment) {
    attachments.remove(attachment);
    attachment.setConversation(null);
  }
}