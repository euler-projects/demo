package net.eulerframework.startweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

@SpringBootApplication
public class StartWebApplication {

	public static void main(String[] args) {
	    SpringApplication.run(StartWebApplication.class, args);
	}

	@Bean
	public ServletRegistrationBean dispatcherRegistration() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet);
		//registration.getUrlMappings().clear();
		registration.addUrlMappings("*.do");
		registration.addUrlMappings("*.json");
		registration.setName("rest");
		return registration;
	}
}
