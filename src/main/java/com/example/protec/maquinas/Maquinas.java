package com.example.protec.maquinas;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "maquinas"
)
public class Maquinas {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String nome;

    @Column(nullable = false, length = 255)
    private String riscos;

    @Column(nullable = false)
    private Boolean atividade;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
    }


    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRiscos() {
        return riscos;
    }

    public Boolean getAtividade() {
        return atividade;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setRiscos(String riscos) {
        this.riscos = riscos;
    }

    public void setAtividade(Boolean atividade)
    {
        this.atividade = atividade;
    }
}