package com.devicehive.application.security;

import com.devicehive.auth.rest.HttpAuthenticationFilter;
import com.devicehive.auth.rest.SimpleCORSFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/css/**", "/server/**", "/scripts/**", "/webjars/**", "/templates/**").permitAll()
//                .antMatchers("/*/swagger.json", "/*/swagger.yaml").permitAll()
                .and()
                .anonymous().disable()
                .exceptionHandling();
                //.authenticationEntryPoint(unauthorizedEntryPoint());

        http
                .addFilterBefore(new HttpAuthenticationFilter(authenticationManager()), BasicAuthenticationFilter.class)
                .addFilterAfter(new SimpleCORSFilter(), HttpAuthenticationFilter.class);
    }
}
