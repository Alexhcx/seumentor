package com.projetointegrador.seumentor.chat.model;

import com.projetointegrador.seumentor.chat.enums.MessageType;
import com.projetointegrador.seumentor.common.model.BaseEntity;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.tutoring.model.Tutoring; // Import Tutoring
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Conversations")
public class Conversations extends BaseEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  @ToString.Exclude
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id") 
  @ToString.Exclude
  private User receiver;

  @Column(columnDefinition = "TEXT", length = 500) 
  private String chatMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tutoring_id") 
  @ToString.Exclude
  private Tutoring tutoring;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MessageType messageType; 

  @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  @ToString.Exclude
  private Set<Attachment> attachments = new HashSet<>();

  public void addAttachment(Attachment attachment) {
    attachments.add(attachment);
    attachment.setConversation(this);
  }

  public void removeAttachment(Attachment attachment) {
    attachments.remove(attachment);
    attachment.setConversation(null);
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    Conversations that = (Conversations) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
  }
}