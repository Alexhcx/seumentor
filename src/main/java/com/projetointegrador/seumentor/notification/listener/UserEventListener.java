package com.projetointegrador.seumentor.notification.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.projetointegrador.seumentor.notification.service.EmailService;
import com.projetointegrador.seumentor.tutoring.api.dto.MentorshipAcceptedEvent;
import com.projetointegrador.seumentor.tutoring.api.dto.MentorshipCancelledEvent;
import com.projetointegrador.seumentor.tutoring.api.dto.MentorshipCompletedEvent;
import com.projetointegrador.seumentor.tutoring.api.dto.MentorshipStartingEvent;
import com.projetointegrador.seumentor.user.api.events.PasswordResetRequestedEvent;
import com.projetointegrador.seumentor.user.api.events.UserRegisteredEvent;

@Component
public class UserEventListener {

  private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

  @Autowired
  private EmailService emailService;

  @EventListener
  @Async
  public void handleUserRegisteredEvent(UserRegisteredEvent event) {
    log.info("Received UserRegisteredEvent for user email: {}", event.email());
    try {
      emailService.enviarEmailBoasVindas(event.email(), event.firstName());
    } catch (Exception e) {
      log.error("Failed to send welcome email for user email {}: {}", event.email(), e.getMessage(), e);
    }
  }

  @EventListener
  @Async
  public void handlePasswordResetRequestedEvent(PasswordResetRequestedEvent event) {
    log.info("Received PasswordResetRequestedEvent for email: {}", event.email());
    try {
      emailService.enviarEmailResetSenha(event.email(), event.firstName(), event.token());
    } catch (Exception e) {
      log.error("Failed to send password reset email for email {}: {}", event.email(), e.getMessage(), e);
    }
  }

  @EventListener
  @Async
  public void handleMentorshipAcceptedEvent(MentorshipAcceptedEvent event) {
    log.info("Received MentorshipAcceptedEvent for mentee email: {}, tutoringId: {}", event.emailMentorado(),
        event.tutoringId());
    try {
      emailService.enviarEmailMentoriaAceita(
          event.emailMentorado(),
          event.nomeMentorado(),
          event.nomeMentor(),
          event.nomeDisciplina());
    } catch (Exception e) {
      log.error("Failed to send mentorship accepted email for mentee email {}: {}", event.emailMentorado(),
          e.getMessage(), e);
    }
  }

  @EventListener
  @Async
  public void handleMentorshipStartingEvent(MentorshipStartingEvent event) {
    log.info("Received MentorshipStartingEvent for mentee email: {}, tutoringId: {}", event.emailMentorado(),
        event.tutoringId());
    try {
      emailService.enviarEmailMentoriaIniciando(
          event.emailMentorado(),
          event.nomeMentorado(),
          event.nomeMentor(),
          event.nomeDisciplina(),
          event.horarioInicio(),
          event.linkMentoria(),
          event.localMentoria(),
          event.tipoMentoria(),
          event.tutoringId());
    } catch (Exception e) {
      log.error("Failed to send mentorship starting email for mentee email {}: {}", event.emailMentorado(),
          e.getMessage(), e);
    }
  }

  @EventListener
  @Async
  public void handleMentorshipCompletedEvent(MentorshipCompletedEvent event) {
    log.info("Received MentorshipCompletedEvent for mentee email: {}, tutoringId: {}", event.emailMentorado(),
        event.tutoringId());
    try {
      emailService.enviarEmailMentoriaConcluidaAvaliar(
          event.emailMentorado(),
          event.nomeMentorado(),
          event.nomeMentor(),
          event.nomeDisciplina(),
          event.tutoringId(),
          event.menteeId());
    } catch (Exception e) {
      log.error("Failed to send mentorship completed/rating request email for mentee email {}: {}",
          event.emailMentorado(), e.getMessage(), e);
    }
  }

  @EventListener
  @Async
  public void handleMentorshipCancelledEvent(MentorshipCancelledEvent event) {
    log.info("Received MentorshipCancelledEvent for recipient email: {}, tutoringId: {}", event.emailDestinatario(),
        event.tutoringId());
    try {
      emailService.enviarEmailMentoriaCancelada(
          event.emailDestinatario(),
          event.nomeDestinatario(),
          event.nomeDisciplina(),
          event.dataMentoria(),
          event.horarioInicioMentoria(),
          event.tutoringId(),
          event.motivoAdicional());
    } catch (Exception e) {
      log.error("Failed to send mentorship cancelled email for recipient email {}: {}", event.emailDestinatario(),
          e.getMessage(), e);
    }
  }
}