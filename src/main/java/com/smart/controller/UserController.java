package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder.In;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	//method will fire in every handler in this class
	@ModelAttribute
	public void addCommonData(Model m,Principal principal) {
		
		String userName = principal.getName();
		System.out.println("USERNAME: "+userName);
		
		//get the user by using username from database(email)
		User user = userRepository.getUserbyUserName(userName);
		System.out.println("USER: "+user);
		m.addAttribute("user", user);
		
	}
	

	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model m,Principal principal) {
		m.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	//open add form handler
	@RequestMapping("/add-contact")
	public String openAddContactForm(Model m) {
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	
	@PostMapping("/process-contact")
	public String processAddContactForm(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file
			,Principal principal,HttpSession session) {
		
	
		try {
			String name = principal.getName();
			User user = userRepository.getUserbyUserName(name);			
			
//			if(3>2) {
//				throw new Exception();
//			}
			
			
			//processing and uploading a file
			if(file.isEmpty()) {
				//if the file is empty then try our message
				System.out.println("File is Empty");
				contact.setImageUrl("contact.png");
			}
			else {
				//upload the file to folder and update the name to contact
				contact.setImageUrl(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is Uploaded");

			}
		
			
			
			user.getContacts().add(contact);
			contact.setUser(user);
			this.userRepository.save(user);
			
			System.out.println("DATA: "+contact);
			System.out.println("added to database");
			
			//message success
			session.setAttribute("message",new Message("Your Contact is added !! Add More", "success") );
			
	} catch (Exception e) {
			
			System.out.println("ERROR: "+e.getMessage());
			e.printStackTrace();
			//message error
			session.setAttribute("message",new Message("Something went wrong !! Try Again", "danger") );
			
		}
		return "normal/add_contact_form";
	}
	
	
	//showcontacts
	//per page=5[n]
	//current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m,Principal principal) {
		
		m.addAttribute("title", "Show User Contacts");
		
		String userName = principal.getName();
		User user=userRepository.getUserbyUserName(userName);
//		List<Contact> contacts = user.getContacts();
		
		//contact ki list bhejna hae
	
		//page request
		Pageable pageable = PageRequest.of(page, 8);
		
		 Page<Contact> contacts=contactRepository.findContactsByUser(user.getId(),pageable);
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		
		m.addAttribute("totalPages", contacts.getTotalPages());
		
		return "normal/show_contact";
	}
	
	//showing particular contact detail	
	@RequestMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model m,Principal principal) {
		
		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//solving security bug
		String userName = principal.getName();
		User user = userRepository.getUserbyUserName(userName);
		
		if(user.getId()==contact.getUser().getId())
		{
			m.addAttribute("title", contact.getName());
			m.addAttribute("contact", contact);
		}
		
		System.out.println("CID: "+cId);
		return "normal/contact_detail";
	}
	
	//delete a contact handler
	@GetMapping("/delete/{cid}")
	@Transactional
	public String deleteContact(@PathVariable("cid") Integer cId,Model m,HttpSession session,Principal principal) {
		
		
		 System.out.println("CID: "+cId);
		 Contact contact = contactRepository.findById(cId).get();
		
	

			User user = userRepository.getUserbyUserName(principal.getName());
			user.getContacts().remove(contact);
			this.userRepository.save(user);
			
			System.out.println("DELETED");
			session.setAttribute("message", new Message("Contact deleted successfully", "success"));

		return "redirect:/user/show-contacts/0";
	}
	
	//open update contact
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId,Model m) {
		
		
		m.addAttribute("title","Update Contact");
		Contact contact = contactRepository.findById(cId).get();
		m.addAttribute("contact", contact);
		
		return "normal/update-form";
	}
	
	//update contact handler
	//@RequestMapping(value = "/process-update" ,method = RequestMethod.POST)
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,Model m,HttpSession session,Principal principal) {
		
		
		try {
			//old contact details
			Contact oldContactDetails = contactRepository.findById(contact.getcId()).get();
			
			//image
			if(!file.isEmpty()) {
				//file work
				//delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldContactDetails.getImageUrl());
				file1.delete();
				
				
				//update new photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImageUrl(file.getOriginalFilename());
				
				System.out.println("Image is Uploaded");
			}
			else {
				contact.setImageUrl(oldContactDetails.getImageUrl());
			}
			
			
			//get user id and update
		User user = userRepository.getUserbyUserName(principal.getName());
		contact.setUser(user);
		contactRepository.save(contact);
		
		//message that contact is updated
		session.setAttribute("message", new Message("Your contact is updated..", "success"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Contact Name:"+ contact.getName());
		System.out.println("Contact Id:" + contact.getcId());
		
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//Your profile handler
	@GetMapping("/profile")
	public String profileHandler(Model m) {
		
		m.addAttribute("title","Your Profile Page");
		return "normal/profile";
	}
	
	//open setting handler
	
	@GetMapping("/settings")
	public String openSettings(Model m) {
		
	return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword,Principal principal,HttpSession session) {
		
		System.out.println("OLD_PASSWORD: "+oldPassword);
		System.out.println("NEW_PASSWORD: "+newPassword);
		
		String userName = principal.getName();
		User currentUser = userRepository.getUserbyUserName(userName);
		System.out.println(currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword()))
		{
			//change the password
			currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			userRepository.save(currentUser);
			
			//success message
			session.setAttribute("message", new Message("Your password has been changed successfully", "success"));
		}
		else 
		{
			//error message...
			session.setAttribute("message", new Message("Please enter correct old password","danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
}
