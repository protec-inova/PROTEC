package com.example.protec.funcionarios;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "funcionarios"
)
public class Funcionarios {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String nome;

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


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
