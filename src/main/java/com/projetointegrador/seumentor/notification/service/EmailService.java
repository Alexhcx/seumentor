package com.projetointegrador.seumentor.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Importar
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.nomeProjeto:SEU MENTOR}")
    private String nomeProjeto;

    @Value("${app.url.base:http://localhost:8080}")
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
}