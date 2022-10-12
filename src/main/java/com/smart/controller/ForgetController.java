package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.services.EmailService;

@Controller
public class ForgetController {

	//inject email service
	@Autowired
	private EmailService emailService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	
	
	Random random=new Random(1000);
	
	//email id open handler
	@GetMapping("/forgot")
	public String opeEmailForm(Model m) {
		return "forgot_email_form";
	}
	
	//post req for forgot password
	@PostMapping("/send-otp")
	public String sendOtp(Model m,@RequestParam("email") String email,HttpSession session) {
		System.out.println("EMAIL: "+email);
		
		//generate random otp
		
		int otp = random.nextInt(999999);
		System.out.println("OTP: "+otp);
		
		//code for send otp to email
		String subject="OTP from SCM";
		
		String message=""
				+"<div style='border:1px solid #e2e2e2; padding:20px;'>"
				+"<h1>"
				+"OTP is"
				+"<b>"+otp
				+"</b>"
				+"</h1>"
				+"</div>";
		
		String to=email;
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		
		if(flag) {
			session.setAttribute("myotp",otp);
			session.setAttribute("email", email);
			return "verify_otp";
			
		}
		else {
			session.setAttribute("message","check your email id !!!");
			return "forgot_email_form";
		}
	}
	
	//verifyotp handler
	@PostMapping("/verify-otp")
	public String otpMatcher(Model m,@RequestParam("otp") int otp,HttpSession session) {
		
		int myOtp=(int)session.getAttribute("myotp");
		String email=(String)session.getAttribute("email");
		
		if(myOtp==otp)
		{
			//fetch user by name
			User user = userRepository.getUserbyUserName(email);
					
					if(user==null) {
						//send error message
						session.setAttribute("message", "User doesn't exist");
						return "verify_otp";
					}
					else {
					//view password_change form as the otp matched
			
			}
					
			return "password_change_form";
		}
		
		else {
			session.setAttribute("message", "You have entered the wrong otp!!,Please correct it.");
			return "verify_otp";
		}
		
	}
	
	//change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword,HttpSession session) {
		System.out.println("NEW PASSWORD: "+newPassword);
		
		String email=(String)session.getAttribute("email");
		User user = userRepository.getUserbyUserName(email);
		
		user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
		userRepository.save(user);
		
		return "redirect:/signin?change=password changed successfully..";
	}
}
