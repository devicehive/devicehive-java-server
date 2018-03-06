package com.devicehive.application.security;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.rest.HttpAuthenticationFilter;
import com.devicehive.auth.rest.SimpleCORSFilter;
import com.devicehive.auth.rest.providers.HiveAnonymousAuthenticationProvider;
import com.devicehive.auth.rest.providers.JwtTokenAuthenticationProvider;
import com.devicehive.model.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private Gson gson = new GsonBuilder().create();

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/css/**", "/server/**", "/scripts/**", "/webjars/**", "/templates/**").permitAll()
                .antMatchers("/*/swagger.json", "/*/swagger.yaml").permitAll()
                .and()
                .anonymous().disable()
                .exceptionHandling()
                .authenticationEntryPoint(unauthorizedEntryPoint());

        http
                .addFilterBefore(new SimpleCORSFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new HttpAuthenticationFilter(authenticationManager()), SimpleCORSFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(jwtTokenAuthenticationProvider())
                .authenticationProvider(anonymousAuthenticationProvider());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public JwtTokenAuthenticationProvider jwtTokenAuthenticationProvider() {
        return new JwtTokenAuthenticationProvider();
    }

    @Bean
    public HiveAnonymousAuthenticationProvider anonymousAuthenticationProvider() {
        return new HiveAnonymousAuthenticationProvider();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().println(
                    gson.toJson(new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage())));
        };
    }
}
