package com.example.demo.service.impl;

import com.example.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String SUBJECT_OTP = "Tu c\u00F3digo de verificaci\u00F3n de Artistlan";
    private static final String SUBJECT_PASSWORD_RESET = "C\u00F3digo de recuperaci\u00F3n de contrase\u00F1a - Artistlan";
    private static final String SMTP_ERROR_MESSAGE = "No se pudo enviar el c\u00F3digo por correo. Revisa la configuraci\u00F3n SMTP.";

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
        enviarCorreo(
                correoDestino,
                SUBJECT_OTP,
                """
                Hola,

                Recibimos una solicitud para iniciar sesi\u00F3n o activar la verificaci\u00F3n en dos pasos (2FA) en tu cuenta de Artistlan.

                Tu c\u00F3digo de verificaci\u00F3n es:
                %s

                Este c\u00F3digo expira en 5 minutos.

                Si no solicitaste este c\u00F3digo, puedes ignorar este mensaje.

                Equipo Artistlan
                """.formatted(codigoFormateado)
        );
    }

    @Override
    public void enviarCodigoRecuperacionContrasena(String correoDestino, String codigo) {
        validarConfiguracionMinimaSmtp();
        String codigoFormateado = formatearCodigo(codigo);
        enviarCorreo(
                correoDestino,
                SUBJECT_PASSWORD_RESET,
                """
                Hola,

                Recibimos una solicitud para recuperar la contrase\u00F1a de tu cuenta de Artistlan.

                Tu c\u00F3digo de recuperaci\u00F3n es:
                %s

                Este c\u00F3digo es v\u00E1lido por 5 minutos.

                Si no solicitaste este cambio, puedes ignorar este mensaje.

                Atentamente,
                Equipo Artistlan
                """.formatted(codigoFormateado)
        );
    }

    private String formatearCodigo(String codigo) {
        if (codigo == null || codigo.length() != 6) {
            return codigo;
        }
        return codigo.substring(0, 3) + " " + codigo.substring(3);
    }

    private void validarConfiguracionMinimaSmtp() {
        if (isBlank(springMailHost)) {
            log.error("Configuraci\u00F3n SMTP incompleta: spring.mail.host vac\u00EDo. Variables esperadas: SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD, ARTISTLAN_MAIL_FROM(opcional).");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, SMTP_ERROR_MESSAGE);
        }

        if (isBlank(mailFrom) && isBlank(springMailUsername)) {
            log.error("Configuraci\u00F3n SMTP incompleta: no hay remitente. Define ARTISTLAN_MAIL_FROM o SPRING_MAIL_USERNAME.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, SMTP_ERROR_MESSAGE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void enviarCorreo(String correoDestino, String subject, String body) {
        String from = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : springMailUsername;
        String toSafe = safeEmail(correoDestino);

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(correoDestino);
        message.setSubject(subject);
        message.setText(body);

        log.info("Intentando envio de correo OTP. to={}, subject={}", toSafe, subject);
        try {
            mailSender.send(message);
            log.info("Correo OTP enviado. to={}, subject={}, smtpHost={}, smtpPort={}",
                    toSafe, subject, springMailHost, springMailPort);
        } catch (MailException ex) {
            log.error("Fallo SMTP al enviar OTP. to={}, subject={}, smtpHost={}, smtpPort={}, userConfigured={}, fromConfigured={}, exceptionType={}, cause={}",
                    toSafe,
                    subject,
                    springMailHost,
                    springMailPort,
                    !isBlank(springMailUsername),
                    !isBlank(mailFrom),
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, SMTP_ERROR_MESSAGE);
        } catch (Exception ex) {
            log.error("Error inesperado al enviar OTP. to={}, subject={}, smtpHost={}, smtpPort={}, exceptionType={}, cause={}",
                    toSafe,
                    subject,
                    springMailHost,
                    springMailPort,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, SMTP_ERROR_MESSAGE);
        }
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
