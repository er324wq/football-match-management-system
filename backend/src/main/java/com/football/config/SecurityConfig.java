package com.football.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.security.JwtAuthenticationFilter;
import com.football.vo.ResultVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter jwtFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, exception) -> writeError(
                        response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "请先登录"))
                .accessDeniedHandler((request, response, exception) -> writeError(
                        response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "无权访问该接口"))
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/login", "/api/user/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/user/list").hasRole("admin")
                .antMatchers(HttpMethod.GET, "/api/user/*").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/user").authenticated()
                .antMatchers(HttpMethod.GET, "/api/match/update-status").hasRole("admin")
                .antMatchers(HttpMethod.GET, "/api/team/user").authenticated()

                // Public read-only football data used by the visitor pages.
                .antMatchers(HttpMethod.GET,
                        "/api/match/list", "/api/match/all", "/api/match/*",
                        "/api/team/list", "/api/team/all", "/api/team/*",
                        "/api/player/list", "/api/player/all", "/api/player/with-team",
                        "/api/player/team/*", "/api/player/*", "/api/team-member/team/*",
                        "/api/statistics/standings", "/api/statistics/top-scorers",
                        "/api/application/match-events", "/api/application/match-event/*/matches",
                        "/api/match-result/list", "/api/match-result/with-match",
                        "/api/goal-record/list", "/api/goal-record/with-details",
                        "/api/card-record/list", "/api/card-record/with-details",
                        "/api/notice/**")
                .permitAll()

                // Administrative resources and destructive match-management actions.
                .antMatchers("/api/user/**", "/api/performance/**").hasRole("admin")
                .antMatchers(HttpMethod.POST, "/api/match/**").hasRole("admin")
                .antMatchers(HttpMethod.PUT, "/api/match/**").hasRole("admin")
                .antMatchers(HttpMethod.DELETE, "/api/match/**").hasRole("admin")
                .antMatchers(HttpMethod.POST, "/api/match-result/**", "/api/goal-record/**", "/api/card-record/**", "/api/scoring/**")
                .hasAnyRole("admin", "referee")
                .antMatchers(HttpMethod.PUT, "/api/match-result/**", "/api/goal-record/**", "/api/card-record/**", "/api/scoring/**")
                .hasAnyRole("admin", "referee")
                .antMatchers(HttpMethod.DELETE, "/api/match-result/**", "/api/goal-record/**", "/api/card-record/**", "/api/scoring/**")
                .hasAnyRole("admin", "referee")
                .antMatchers(HttpMethod.GET, "/api/scoring/referee").hasAnyRole("admin", "referee")
                .antMatchers(HttpMethod.GET, "/api/scoring/all").hasRole("admin")
                .antMatchers(HttpMethod.POST, "/api/team/**", "/api/player/**", "/api/team-member/**")
                .hasRole("admin")
                .antMatchers(HttpMethod.PUT, "/api/team/**", "/api/player/**", "/api/team-member/**")
                .hasRole("admin")
                .antMatchers(HttpMethod.DELETE, "/api/team/**", "/api/player/**", "/api/team-member/**")
                .hasRole("admin")

                // Application review and event arrangement are administrator operations.
                .antMatchers(HttpMethod.PUT, "/api/application/status/**")
                .hasRole("admin")
                .antMatchers(HttpMethod.GET, "/api/application/all")
                .hasRole("admin")
                .antMatchers(HttpMethod.POST, "/api/application/match-event/*/random-arrange",
                        "/api/application/match-event/*/team/*/lineup")
                .hasRole("admin")

                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, ObjectMapper objectMapper, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ResultVO.error(status, message)));
    }
}
