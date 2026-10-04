package com.ssg.seatreserv.sercurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ssg.seatreserv.sercurity.filter.TokenAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final TokenAuthenticationFilter tokenAuthenticationFilter;

	public SecurityConfig(TokenAuthenticationFilter tokenAuthenticationFilter) {
		this.tokenAuthenticationFilter = tokenAuthenticationFilter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		return http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(
						auth -> auth.requestMatchers("/user/createUser","/createShow","/show/**").permitAll().anyRequest().authenticated())
				.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class).build();
	}
}
