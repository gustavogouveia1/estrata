package com.estrata.estrata_mvp.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * ARQUIVO: CoreDomain.java
 * DESCRIÇÃO: Contém as Entidades fundamentais do sistema respeitando a hierarquia
 * e relacionamentos do negócio.
 */

// --- ENUMS (Hierarquia Rigorosa) ---

enum Role {
    AUXILIAR_TECNICO(1),
    ASSISTENTE_TECNICO(2),
    ANALISTA_TECNICO(3),
    LIDER_PROJETOS(4),
    RH(5),
    DIRETOR(6),
    ADMIN(99),
    DEV(100);

    private final int level;

    Role(int level) { this.level = level; }

    public int getLevel() { return level; }

    // Método utilitário para verificar hierarquia
    public boolean hasAuthorityOver(Role other) {
        return this.level > other.level;
    }
}

enum ProjectStatus {
    PLANEJAMENTO, EM_EXECUCAO, PAUSADO, CONCLUIDO
}

enum TaskStatus {
    PENDENTE, EM_EXECUCAO, BLOQUEADA, CONCLUIDA
}

// --- ENTIDADES ---

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private boolean active = true;
}

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Relacionamento com Equipe (pode ser nulo se for escritório)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private DrillingTeam team;
}

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
class Project extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String clientName;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager; // Líder de Projetos ou superior

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private Set<Task> tasks = new HashSet<>();

    // Lista de boletins associados
    @OneToMany(mappedBy = "project")
    private Set<TechnicalBulletin> bulletins = new HashSet<>();
}

@Entity
@Table(name = "drilling_teams")
@Getter @Setter
class DrillingTeam extends BaseEntity {
    private String name;

    @OneToOne
    private User leader; // Encarregado da equipe

    @OneToMany(mappedBy = "team")
    private Set<User> members = new HashSet<>();
}

@Entity
@Table(name = "tasks")
@Getter @Setter
class Task extends BaseEntity {

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToOne(optional = false)
    private Project project;

    @ManyToOne(optional = false)
    private User mainResponsible;

    @ManyToMany
    @JoinTable(
            name = "task_collaborators",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> collaborators = new HashSet<>(); // Máximo 2 validado no Service
}

// Classe base para Boletins (Polimorfismo)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "bulletins")
@Getter @Setter
abstract class TechnicalBulletin extends BaseEntity {

    @ManyToOne(optional = false)
    private Project project;

    @ManyToOne(optional = false)
    private User author;

    private LocalDateTime executionDate;

    // URL do PDF gerado (armazenado em S3 ou disco local)
    private String generatedPdfPath;

    public abstract String getBulletinType();
}