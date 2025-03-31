// Ex: src/main/java/com/projetointegrador/seumentor/notification/listener/UserEventListener.java
package com.projetointegrador.seumentor.notification.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.projetointegrador.seumentor.notification.service.EmailService;
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

}