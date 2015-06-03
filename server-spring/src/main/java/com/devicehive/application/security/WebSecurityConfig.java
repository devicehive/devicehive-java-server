package com.devicehive.application.security;

import com.devicehive.auth.rest.HttpAuthenticationFilter;
import com.devicehive.auth.rest.providers.AccessTokenAuthenticationProvider;
import com.devicehive.auth.rest.providers.BasicAuthenticationProvider;
import com.devicehive.auth.rest.providers.DeviceAuthenticationProvider;
import com.devicehive.auth.rest.providers.HiveAnonymousAuthenticationProvider;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
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
import java.util.Optional;

@Configuration
@EnableWebSecurity
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private Gson gson = new GsonBuilder().create();

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                    .antMatchers("/css/**", "/scripts/**", "/webjars/**", "/templates/**").permitAll()
                .and()
                .anonymous().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedEntryPoint());

        http
                .addFilterBefore(new HttpAuthenticationFilter(authenticationManager()), BasicAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(basicAuthenticationProvider())
                .authenticationProvider(deviceAuthenticationProvider())
                .authenticationProvider(accessTokenAuthenticationProvider())
                .authenticationProvider(anonymousAuthenticationProvider());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public BasicAuthenticationProvider basicAuthenticationProvider() {
        return new BasicAuthenticationProvider();
    }

    @Bean
    public DeviceAuthenticationProvider deviceAuthenticationProvider() {
        return new DeviceAuthenticationProvider();
    }

    @Bean
    public AccessTokenAuthenticationProvider accessTokenAuthenticationProvider() {
        return new AccessTokenAuthenticationProvider();
    }

    @Bean
    public HiveAnonymousAuthenticationProvider anonymousAuthenticationProvider() {
        return new HiveAnonymousAuthenticationProvider();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            Optional<String> authHeader = Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION));
            if (authHeader.isPresent() && authHeader.get().startsWith(Constants.OAUTH_AUTH_SCEME)) {
                response.addHeader(HttpHeaders.WWW_AUTHENTICATE, Messages.OAUTH_REALM);
            } else {
                response.addHeader(HttpHeaders.WWW_AUTHENTICATE, Messages.BASIC_REALM);
            }
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().println(
                    gson.toJson(new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage())));
        };
    }


}
