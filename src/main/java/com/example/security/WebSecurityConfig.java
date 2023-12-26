package com.example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(c -> c.ignoringRequestMatchers("auth/login", "auth/logout")
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.cors(c -> c.disable())
				.authorizeHttpRequests((requests) -> requests
						.requestMatchers("/", "/css/**", "js/**", "/image/**").permitAll()
						.requestMatchers("/*.ico").permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin((form) -> form
						.loginPage("/auth/login")
						.permitAll())
				.logout((logout) -> {
					logout
							.logoutSuccessUrl("/auth/login?logout");
					String logoutUrl = "/auth/logout";
					logout.logoutRequestMatcher(
							new OrRequestMatcher(
									new AntPathRequestMatcher(logoutUrl, "GET"),
									new AntPathRequestMatcher(logoutUrl, "POST"),
									new AntPathRequestMatcher(logoutUrl, "PUT"),
									new AntPathRequestMatcher(logoutUrl, "DELETE")))
							.invalidateHttpSession(true)
							.clearAuthentication(true);
					logout.permitAll();
				});

		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 追加
		UserDetails user = User.builder()
				.username("user")
				.password(passwordEncoder.encode("password"))
				.roles("USER")
				.build();

		UserDetails admin = User.builder()
				.username("admin")
				.password(passwordEncoder.encode("password"))
				.roles("USER", "ADMIN")
				.build();

		return new CustomInMemoryUserDetailsManager(user, admin);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
