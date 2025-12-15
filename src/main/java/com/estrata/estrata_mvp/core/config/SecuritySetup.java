package com.estrata.estrata_mvp.core.config;

import com.estrata.estrata_mvp.domain.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ARQUIVO: SecuritySetup.java
 * DESCRIÇÃO: Configuração de segurança.
 * Implementa a proteção baseada nos cargos hierárquicos definidos.
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Permite usar @PreAuthorize nos Controllers
public class SecuritySetup {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desabilitado para MVP/API REST; habilitar em Prod com Cookie
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()

                        // Regras Hierárquicas Globais (Exemplos)
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers("/api/hr/**").hasAnyAuthority(Role.RH.name(), Role.DIRETOR.name(), Role.ADMIN.name())

                        // Gestão de Projetos: Apenas Analistas e acima podem CRIAR
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/projects/**")
                        .hasAnyAuthority(Role.ANALISTA_TECNICO.name(), Role.LIDER_PROJETOS.name(), Role.DIRETOR.name())

                        // Todos autenticados podem ver projetos (se associados)
                        .anyRequest().authenticated()
                )
                // Configuração simples de sessão para MVP (pode evoluir para JWT Stateless)
                .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Em um cenário real, aqui viriam os Beans de AuthenticationManager e UserDetailsService
    // conectando ao UserRepository.
}