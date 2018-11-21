package com.quant.backtest.multi.strategy.utils;

import java.time.LocalDate;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.quant.backtest.multi.strategy.cache.EmailCache;

@Component
public class EmailUtils {

    private static final Logger logger = LoggerFactory.getLogger(EmailUtils.class);
    
    @Value("${email.to}")
    private String toEmail;
    
    @Autowired
    private EmailCache emailCache;

    public void sendEmail() {
	try {
	    Properties props = new Properties();
	    props.put("mail.smtp.host", "true");
	    props.put("mail.smtp.starttls.enable", "true");
	    props.put("mail.smtp.host", "smtp.gmail.com");
	    props.put("mail.smtp.port", "587");
	    props.put("mail.smtp.auth", "true");
	    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
		/**protected PasswordAuthentication getPasswordAuthentication() {
		    return new PasswordAuthentication("northernmedallionfund@gmail.com", "Medallion@123");*/
		protected PasswordAuthentication getPasswordAuthentication() {
			    return new PasswordAuthentication("dkfc.quant@gmail.com", "dkam1234");
		}
	    });
	    MimeMessage msg = new MimeMessage(session);
	    InternetAddress[] address = InternetAddress.parse(toEmail, true);
	    msg.setRecipients(Message.RecipientType.TO, address);
	    msg.setSubject("DKFC quant managed account - " + LocalDate.now());
	    msg.setSentDate(new Date());
	    msg.setText(emailCache.getValue());
	    msg.setHeader("XPriority", "1");
	    Transport.send(msg);
	    logger.debug("Mail has been sent successfully");
	} catch (Exception e) {
	    logger.error("Error sending email. Error {}", e);
	}
    }
}
