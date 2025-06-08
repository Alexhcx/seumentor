package com.projetointegrador.seumentor.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Importar
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.nomeProjeto:SEU MENTOR}")
    private String nomeProjeto;

    @Value("${app.url.base:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.email.from:seumentor0@gmail.com}")
    private String emailFrom;

    @Value("${app.url.ajuda:/ajuda}")
    private String pathAjuda;

    @Value("${app.url.cancelamento:/cancelar-inscricao}")
    private String pathCancelamento;

    @Value("${app.nomeEmpresa:Seu Mentor Inc.}")
    private String nomeEmpresa;

    @Value("${app.enderecoEmpresa: }")
    private String enderecoEmpresa;

    @Value("${app.url.resetPasswordPath:/reset-password-page}")
    private String pathResetPassword;

    @Async
    public void enviarEmailResetSenha(String destinatario, String nomeUsuario, String token) {
        log.info("Tentando enviar email de redefinição de senha para: {}", destinatario);
        try {
            Context context = new Context();

            context.setVariable("nomeUsuario", nomeUsuario);
            context.setVariable("nomeProjeto", this.nomeProjeto);
            String linkReset = this.baseUrl + this.pathResetPassword + "?token=" + token;
            context.setVariable("linkReset", linkReset);
            context.setVariable("token", token);
            context.setVariable("baseUrl", this.baseUrl);
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);

            String corpoHtml = templateEngine.process("reset-senha", context);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Redefinição de Senha - " + this.nomeProjeto);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de redefinição de senha enviado async para: {}", destinatario);

        } catch (MessagingException e) {
            log.error("Erro de MimeMessage ao enviar email de redefinição para {}: {}", destinatario, e.getMessage(),
                    e);
        } catch (Exception e) {
            log.error("Erro geral ao enviar email de redefinição para {}: {}", destinatario, e.getMessage(), e);
        }
    }

    @Async
    public void enviarEmailBoasVindas(String destinatario, String nomeUsuario) {
        try {
            Context context = new Context();

            context.setVariable("nomeUsuario", nomeUsuario);
            context.setVariable("nomeProjeto", this.nomeProjeto);
            context.setVariable("linkProjeto", this.baseUrl + "/login");
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);
            context.setVariable("linkCancelamento", this.baseUrl + this.pathCancelamento + "?email=" + destinatario);

            String corpoHtml = templateEngine.process("boas-vindas", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Bem-vindo(a) ao " + this.nomeProjeto + "!");
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            System.out.println("Email de boas-vindas enviado async para: " + destinatario);

        } catch (Exception e) {
            System.err.println("Erro ao enviar email de boas-vindas para " + destinatario + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void enviarEmailMentoriaAceita(String destinatario, String nomeUsuario, String nomeMentor,
            String nomeDisciplina) {
        log.info("Tentando enviar email de notificação de mentoria aceita para: {}", destinatario);
        try {
            Context context = new Context();
            context.setVariable("nomeUsuario", nomeUsuario);
            context.setVariable("nomeMentor", nomeMentor);
            context.setVariable("nomeDisciplina", nomeDisciplina);
            context.setVariable("nomeProjeto", this.nomeProjeto);
            context.setVariable("linkProjeto", this.baseUrl + "/perfil");
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);
            // O link de cancelamento pode não ser relevante aqui, ou pode levar para
            // configurações de notificação
            context.setVariable("linkCancelamento", this.baseUrl + "/perfil");

            String corpoHtml = templateEngine.process("mentoria-aceita", context); // Novo template
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Sua solicitação de mentoria foi aceita! - " + this.nomeProjeto);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de notificação de mentoria aceita enviado async para: {}", destinatario);

        } catch (MessagingException e) {
            log.error("Erro de MimeMessage ao enviar email de mentoria aceita para {}: {}", destinatario,
                    e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro geral ao enviar email de mentoria aceita para {}: {}", destinatario, e.getMessage(), e);
        }
    }

    @Async
    public void enviarEmailMentoriaIniciando(String destinatario, String nomeUsuario, String nomeMentor,
            String nomeDisciplina, String horarioInicio, String linkMentoria, String localMentoria,
            TutoringClassType tipoMentoria, Long tutoringId) {
        log.info("Tentando enviar email de notificação de mentoria iniciando para: {}, tutoringId: {}", destinatario,
                tutoringId);
        try {
            Context context = new Context();
            context.setVariable("nomeUsuario", nomeUsuario);
            context.setVariable("nomeMentor", nomeMentor);
            context.setVariable("nomeDisciplina", nomeDisciplina);
            context.setVariable("horarioInicio", horarioInicio);
            context.setVariable("linkMentoria", linkMentoria);
            context.setVariable("localMentoria", localMentoria);
            context.setVariable("isOnline", tipoMentoria == TutoringClassType.ONLINE);
            context.setVariable("isPresencial", tipoMentoria == TutoringClassType.PRESENCIAL);

            context.setVariable("nomeProjeto", this.nomeProjeto);
            context.setVariable("linkDetalhesMentoria", this.baseUrl + "/perfil");
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);

            String corpoHtml = templateEngine.process("mentoria-iniciando", context);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Sua mentoria de " + nomeDisciplina + " está começando! - " + this.nomeProjeto);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de notificação de mentoria iniciando enviado async para: {}, tutoringId: {}", destinatario,
                    tutoringId);

        } catch (MessagingException e) {
            log.error("Erro de MimeMessage ao enviar email de mentoria iniciando para {} (tutoringId: {}): {}",
                    destinatario, tutoringId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro geral ao enviar email de mentoria iniciando para {} (tutoringId: {}): {}", destinatario,
                    tutoringId, e.getMessage(), e);
        }
    }

    @Async
    public void enviarEmailMentoriaConcluidaAvaliar(String destinatario, String nomeUsuario, String nomeMentor,
            String nomeDisciplina, Long tutoringId, Long menteeId) {
        log.info("Tentando enviar email de mentoria concluída e solicitação de avaliação para: {}, tutoringId: {}",
                destinatario, tutoringId);
        try {
            Context context = new Context();
            context.setVariable("nomeUsuario", nomeUsuario);
            context.setVariable("nomeMentor", nomeMentor);
            context.setVariable("nomeDisciplina", nomeDisciplina);
            context.setVariable("tutoringId", tutoringId); // Para construir o link no template

            context.setVariable("nomeProjeto", this.nomeProjeto);
            // Link para a página de avaliação da mentoria específica
            // Ajuste este link conforme a rota da sua aplicação para avaliação
            context.setVariable("linkAvaliarMentoria", this.baseUrl + "/perfil"); // Exemplo
                                                                                  // de link
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);

            String corpoHtml = templateEngine.process("mentoria-concluida-avaliar", context); // Novo template
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject(
                    "Mentoria de " + nomeDisciplina + " concluída! Avalie sua experiência - " + this.nomeProjeto);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de mentoria concluída e solicitação de avaliação enviado async para: {}, tutoringId: {}",
                    destinatario, tutoringId);

        } catch (MessagingException e) {
            log.error("Erro de MimeMessage ao enviar email de mentoria concluída para {} (tutoringId: {}): {}",
                    destinatario, tutoringId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro geral ao enviar email de mentoria concluída para {} (tutoringId: {}): {}", destinatario,
                    tutoringId, e.getMessage(), e);
        }
    }

    @Async
    public void enviarEmailMentoriaCancelada(String destinatario, String nomeDestinatario, String nomeDisciplina,
            String dataMentoria, String horarioInicioMentoria, Long tutoringId, String motivoAdicional) {
        log.info("Tentando enviar email de notificação de mentoria cancelada para: {}, tutoringId: {}", destinatario,
                tutoringId);
        try {
            Context context = new Context();
            context.setVariable("nomeDestinatario", nomeDestinatario);
            context.setVariable("nomeDisciplina", nomeDisciplina);
            context.setVariable("dataMentoria", dataMentoria);
            context.setVariable("horarioInicioMentoria", horarioInicioMentoria);
            context.setVariable("motivoAdicional", motivoAdicional); 

            context.setVariable("nomeProjeto", this.nomeProjeto);
            // Link para a página de mentorias do usuário
            context.setVariable("linkMinhasMentorias", this.baseUrl + "/perfil"); 
            context.setVariable("linkBuscarMentorias", this.baseUrl + "/discipline"); 
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);

            String corpoHtml = templateEngine.process("mentoria-cancelada", context); 
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Mentoria de " + nomeDisciplina + " Cancelada - " + this.nomeProjeto);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de notificação de mentoria cancelada enviado async para: {}, tutoringId: {}", destinatario,
                    tutoringId);

        } catch (MessagingException e) {
            log.error("Erro de MimeMessage ao enviar email de mentoria cancelada para {} (tutoringId: {}): {}",
                    destinatario, tutoringId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro geral ao enviar email de mentoria cancelada para {} (tutoringId: {}): {}", destinatario,
                    tutoringId, e.getMessage(), e);
        }
    }

     @Async
    public void enviarEmailMentoriaSolicitada(String destinatario, String nomeMentor, String nomeMentorado, String nomeDisciplina, Long tutoringId) {
        log.info("Tentando enviar email de nova solicitação de mentoria para: {}", destinatario);
        try {
            Context context = new Context();
            context.setVariable("nomeMentor", nomeMentor);
            context.setVariable("nomeMentorado", nomeMentorado);
            context.setVariable("nomeDisciplina", nomeDisciplina);
            context.setVariable("nomeProjeto", this.nomeProjeto);
            context.setVariable("linkMentoria", this.baseUrl + "/perfil");
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);

            String corpoHtml = templateEngine.process("mentoria-solicitada", context);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Nova Solicitação de Mentoria: " + nomeDisciplina);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de solicitação de mentoria enviado async para: {}", destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email de solicitação de mentoria para {}: {}", destinatario, e.getMessage(), e);
        }
    }

    @Async
    public void enviarEmailNovoParticipante(String destinatario, String nomeMentor, String nomeNovoParticipante, String nomeDisciplina, Long tutoringId) {
        log.info("Tentando enviar email de novo participante para mentoria {} para: {}", tutoringId, destinatario);
        try {
            Context context = new Context();
            context.setVariable("nomeMentor", nomeMentor);
            context.setVariable("nomeNovoParticipante", nomeNovoParticipante);
            context.setVariable("nomeDisciplina", nomeDisciplina);
            context.setVariable("nomeProjeto", this.nomeProjeto);
            context.setVariable("linkMentoria", this.baseUrl + "/perfil");
            context.setVariable("linkAjuda", this.baseUrl + this.pathAjuda);
            context.setVariable("nomeEmpresa", this.nomeEmpresa);
            context.setVariable("enderecoEmpresa", this.enderecoEmpresa);

            String corpoHtml = templateEngine.process("participante-entrou", context);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setTo(destinatario);
            helper.setSubject("Novo Participante na sua Mentoria de " + nomeDisciplina);
            helper.setText(corpoHtml, true);
            helper.setFrom(this.emailFrom);

            mailSender.send(mimeMessage);
            log.info("Email de novo participante enviado async para: {}", destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email de novo participante para {}: {}", destinatario, e.getMessage(), e);
        }
    }
}