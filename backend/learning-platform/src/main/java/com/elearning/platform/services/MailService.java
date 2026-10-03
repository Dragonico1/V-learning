package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correo. Si no hay SMTP configurado devuelve false (no finge éxito)
 * para que quien llama use el canal alterno (RF-011).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final ObjectProvider<JavaMailSender> proveedor;
    private final VlearningProperties props;

    public boolean enviar(String destino, String asunto, String cuerpo) {
        JavaMailSender sender = proveedor.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP no configurado: no se envió el correo a {} (asunto: {})", destino, asunto);
            return false;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(props.mail().remitente());
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            sender.send(mensaje);
            return true;
        } catch (MailException e) {
            log.error("Falló el envío de correo a {}: {}", destino, e.getMessage());
            return false;
        }
    }
}
