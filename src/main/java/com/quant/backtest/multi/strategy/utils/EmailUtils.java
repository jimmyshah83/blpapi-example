package com.quant.backtest.multi.strategy.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailUtils {

    private static final Logger logger = LoggerFactory.getLogger(EmailUtils.class);
    
    @Value("${email.to}")
    private String toEmail;

    public void sendEmail(String text) {
	try {
	    Properties props = new Properties();
	    props.put("mail.smtp.host", "true");
	    props.put("mail.smtp.starttls.enable", "true");
	    props.put("mail.smtp.host", "smtp.gmail.com");
	    props.put("mail.smtp.port", "587");
	    props.put("mail.smtp.auth", "true");
	    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
		protected PasswordAuthentication getPasswordAuthentication() {
		    return new PasswordAuthentication("northernmedallionfund@gmail.com", "Medallion@123");
		}
	    });
	    MimeMessage msg = new MimeMessage(session);
	    InternetAddress[] address = InternetAddress.parse(toEmail, true);
	    msg.setRecipients(Message.RecipientType.TO, address);
	    String timeStamp = new SimpleDateFormat("yyyymmdd hh-mm").format(new Date());
	    msg.setSubject("Quant fund daily portfolio : " + timeStamp);
	    msg.setSentDate(new Date());
	    msg.setText(text);
	    msg.setHeader("XPriority", "1");
	    Transport.send(msg);
	    logger.debug("Mail has been sent successfully with text {}", text);
	} catch (Exception e) {
	    logger.error("Error sending email. Error {}", e);
	}
    }
}
