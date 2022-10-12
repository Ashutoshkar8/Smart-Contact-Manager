package com.smart.services;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;



@Service
public class EmailService {
	
	public boolean sendEmail(String subject,String message,String to) {
		
		//rest of the code
		boolean f=false;
		
		String from="clanrush1469@gmail.com";
		
		//variable for email
		String host="smtp.gmail.com";
		
		//get the system properties
		Properties properties=System.getProperties();
		System.out.println("PROPERTIES: "+properties);
		
		//setting important information to properties object
	
		//host set
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.ssl.enable", "true");
		properties.put("mail.smtp.auth", "true");
		
		//step1:Get the session
		Session session=Session.getInstance(properties, new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				
				return new PasswordAuthentication("clanrush1469@gmail.com", "sowgrhfjatvhwnvc");
			}
			
		});
		session.setDebug(true);
		
		//step 2:compose the message
		MimeMessage m=new MimeMessage(session);
		
		
		try {
		//from email
		m.setFrom(from);
		
		//adding recipient to message
		m.addRecipient(javax.mail.Message.RecipientType.TO,new InternetAddress(to));
		
		//adding subject to message
		m.setSubject(subject);
		
		//adding text to message
//		m.setText(message);
		m.setContent(message,"text/html");
		
		//send
		
		//step3:send the message using transport class
		Transport.send(m);
		
		System.out.println("Sent success...");
		
		f=true;
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
		return f;
	}
}
