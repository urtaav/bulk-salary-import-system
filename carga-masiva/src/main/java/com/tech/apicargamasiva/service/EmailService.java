package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.dto.JobStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envía correo cuando una importación se completa exitosamente.
     */
    @Async("importacionExecutor")
    public void enviarEmailFinalizacion(String email, JobStatusDTO jobStatus) {
        try {
            String htmlContent = generarHtmlFinalizacion(jobStatus);
            enviarEmail(email, "✅ Importación Completada - " + jobStatus.getJobId(), htmlContent);
            log.info("Correo de finalización enviado a {}", email);
        } catch (Exception e) {
            log.error("Error enviando correo de finalización: {}", e.getMessage(), e);
        }
    }

    /**
     * Envía correo cuando ocurre un error en la importación.
     */
    @Async("importacionExecutor")
    public void enviarEmailError(String email, String jobId, String errorMessage) {
        try {
            String htmlContent = generarHtmlError(jobId, errorMessage);
            enviarEmail(email, "❌ Error en Importación - " + jobId, htmlContent);
            log.info("Correo de error enviado a {}", email);
        } catch (Exception e) {
            log.error("Error enviando correo de error: {}", e.getMessage(), e);
        }
    }

    /**
     * Método genérico para enviar correos HTML.
     */
    private void enviarEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom("noreply@empresa.com");

        mailSender.send(message);
    }

    /**
     * Genera el contenido HTML para un correo de finalización.
     */
    private String generarHtmlFinalizacion(JobStatusDTO jobStatus) {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Notificación de Importación</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f6f8; font-family: 'Segoe UI', Roboto, Arial, sans-serif;">

  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td align="center" style="padding: 30px 0;">
        
        <table role="presentation" width="380" cellspacing="0" cellpadding="0" border="0" style="background-color:#ffffff; border-radius:4px; box-shadow:0 1px 3px rgba(0,0,0,0.1); overflow:hidden; border: 1px solid #e0e0e0;">
          
          <tr>
            <td align="center" style="padding: 20px 20px 10px 20px; border-bottom: 2px dashed #e0e0e0;">
              <h1 style="margin:0; font-size:18px; color:#212121; font-weight:700;">TECH API CARGA MASIVA</h1>
            </td>
          </tr>

          <tr>
            <td align="center" style="padding: 15px 20px 10px 20px;">
              <p style="font-size:14px; color:#333333; margin: 0;">
                **NOTIFICACIÓN DE PROCESO COMPLETADO**
              </p>
            </td>
          </tr>

          <tr>
            <td style="padding: 0 20px;">
              <div style="border-top: 1px dashed #bdbdbd;"></div>
            </td>
          </tr>

          <tr>
            <td style="padding: 10px 20px 20px 20px;">
              <p style="font-size:15px; color:#333333; margin-bottom: 15px; font-weight: 600;">
                RESUMEN DE IMPORTACIÓN
              </p>
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="2" style="font-size:13px; color:#444; font-family: 'Courier New', monospace;">
                
                <tr>
                  <td style="width:50%%;">ID DE PROCESO:</td>
                  <td align="right">#%s</td>
                </tr>
                <tr>
                  <td>NOMBRE DE ARCHIVO:</td>
                  <td align="right">%s</td>
                </tr>
                
                <tr><td colspan="2" style="padding-top: 8px;"><div style="border-top: 1px dashed #e0e0e0;"></div></td></tr>

                <tr>
                  <td style="font-weight:700;">TOTAL REGISTROS:</td>
                  <td align="right" style="font-weight:700;">%d</td>
                </tr>
                <tr>
                  <td style="color:#2e7d32; font-weight:700;">REGISTROS EXITOSOS:</td>
                  <td align="right" style="color:#2e7d32; font-weight:700;">%d</td>
                </tr>
                <tr>
                  <td style="color:#c62828; font-weight:700;">REGISTROS CON ERROR:</td>
                  <td align="right" style="color:#c62828; font-weight:700;">%d</td>
                </tr>

                <tr><td colspan="2" style="padding-top: 8px;"><div style="border-top: 1px dashed #e0e0e0;"></div></td></tr>

                <tr>
                  <td style="font-weight:700;">ESTADO FINAL:</td>
                  <td align="right" style="text-transform:uppercase; font-weight:700;">%s</td>
                </tr>
              </table>
            </td>
          </tr>

          <tr>
            <td style="padding: 0 20px;">
              <div style="border-top: 2px dashed #e0e0e0;"></div>
            </td>
          </tr>

          <tr>
            <td align="center" style="padding: 20px 20px 30px 20px;">
              <a href="#" style="display:inline-block; background-color:#1976d2; color:#ffffff; text-decoration:none; padding:10px 18px; border-radius:4px; font-weight:600; font-size:14px;">
                VER REPORTE COMPLETO
              </a>
              <p style="font-size:12px; color:#757575; margin-top: 15px; margin-bottom: 0;">
                Este es un mensaje automático, por favor no responda a este correo.
              </p>
            </td>
          </tr>

          <tr>
            <td style="background-color:#f4f6f8; padding:15px 20px; text-align:center; font-size:11px; color:#777; border-top: 1px solid #e0e0e0;">
              © 2025 Tech API Carga Masiva.
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>

</body>
</html>
""".formatted(
                jobStatus.getJobId(),      // %s (String)
                jobStatus.getFilename(),   // %s (String)
                jobStatus.getTotalRegistros(), // %d (int)
                jobStatus.getExitosos(),   // %d (int)
                jobStatus.getErrores(),    // %d (int)
                jobStatus.getStatus().getDescripcion() // %s (String)
        );
    }

    /**
     * Genera el contenido HTML para un correo de error.
     */
    private String generarHtmlError(String jobId, String errorMessage) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#c62828;">❌ Error en Importación</h2>
                    <p><b>ID del proceso:</b> %s</p>
                    <p><b>Mensaje de error:</b></p>
                    <pre style="background-color:#f8d7da; padding:10px; border-radius:5px; color:#721c24;">%s</pre>
                    <hr/>
                    <p style="font-size:12px; color:#777;">Este es un mensaje automático, no responda a este correo.</p>
                </body>
                </html>
                """.formatted(jobId, errorMessage);
    }
}