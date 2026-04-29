package com.example.demo.service.impl;

import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${artistlan.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String springMailHost;

    @Value("${spring.mail.port:}")
    private String springMailPort;

    @Value("${spring.mail.username:}")
    private String springMailUsername;

    @Override
    public void enviarCodigoVerificacion(String correoDestino, String codigo) {
        validarConfiguracionMinimaSmtp();

        String codigoFormateado = formatearCodigo(codigo);
        String from = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : springMailUsername;

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(correoDestino);
        message.setSubject("Verificacion de seguridad - Artistlan");
        message.setText("""
                Hola,

                Bienvenido a Artistlan 🎨

                Tu codigo de verificacion es:

                %s

                Este codigo expirara en 5 minutos.

                Si no solicitaste este codigo, puedes ignorar este mensaje.

                - Equipo Artistlan
                """.formatted(codigoFormateado));

        try {
            mailSender.send(message);
            log.info("OTP enviado por correo. to={}, from={}, smtpHost={}, smtpPort={}",
                    safeEmail(correoDestino), safeEmail(from), springMailHost, springMailPort);
        } catch (MailException ex) {
            log.error("Fallo SMTP al enviar OTP. to={}, from={}, smtpHost={}, smtpPort={}, userConfigured={}, fromConfigured={}, cause={}",
                    safeEmail(correoDestino),
                    safeEmail(from),
                    springMailHost,
                    springMailPort,
                    !isBlank(springMailUsername),
                    !isBlank(mailFrom),
                    ex.getMessage(),
                    ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el codigo por correo");
        } catch (Exception ex) {
            log.error("Error inesperado al enviar OTP. to={}, from={}, smtpHost={}, smtpPort={}, cause={}",
                    safeEmail(correoDestino),
                    safeEmail(from),
                    springMailHost,
                    springMailPort,
                    ex.getMessage(),
                    ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el codigo por correo");
        }
    }

    private String formatearCodigo(String codigo) {
        if (codigo == null || codigo.length() != 6) {
            return codigo;
        }
        return codigo.substring(0, 3) + " " + codigo.substring(3);
    }

    private void validarConfiguracionMinimaSmtp() {
        if (isBlank(springMailHost)) {
            log.error("Configuracion SMTP incompleta: spring.mail.host vacio. Variables esperadas: SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD, ARTISTLAN_MAIL_FROM(opcional).");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el codigo por correo");
        }

        if (isBlank(mailFrom) && isBlank(springMailUsername)) {
            log.error("Configuracion SMTP incompleta: no hay remitente. Define ARTISTLAN_MAIL_FROM o SPRING_MAIL_USERNAME.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo enviar el codigo por correo");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(vacio)";
        }

        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 1) {
            return "***";
        }

        return trimmed.charAt(0) + "***" + trimmed.substring(at);
    }
}
