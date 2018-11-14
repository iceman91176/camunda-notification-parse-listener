package de.witcom.bpm.tasklistener.notification;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MailNotifier {
	
	
	private String smtpHost = "wit-ex01-12r2.witcom.net";
	private String smtpSender = "no-reply@witcom.de";
	private String templateDir = "/home/carsten/";
	
	public  MailNotifier() {
		//Get config from ENV
		
	}
	
	public void SendTaskNotification(String taskId,String template,String subject,UserRepresentation recipient) {
		
		
        try {
        	//Set the properties
            Properties props = new Properties();
            //Change it with appropriate SMTP host
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", 25);
            Message message = new MimeMessage(Session.getInstance(props, null));
			message.setFrom(new InternetAddress(smtpSender));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.getEmail()));
		    message.setSubject(subject);
			
		    //this.parseTemplate(taskId, template, recipient);
		    
		    BodyPart body = new MimeBodyPart();
            body.setContent(this.parseTemplate(taskId, template, recipient).toString(), "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(body);
            message.setContent(multipart);
            Transport.send(message);
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     
	}
	
	private Writer parseTemplate(String taskId,String templateName,UserRepresentation recipient) throws IOException, TemplateException {
		
		Configuration cfg = new Configuration();
		cfg.setDirectoryForTemplateLoading(new File(this.templateDir));
		
		cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template template = cfg.getTemplate(templateName);

        Map paramMap = new HashMap();
        paramMap.put("taskId", taskId);
        paramMap.put("firstName", recipient.getFirstName());
        paramMap.put("lastName", recipient.getLastName());
        Writer out = new StringWriter();
        
        template.process(paramMap, out);

        return out;

		
	}

}
