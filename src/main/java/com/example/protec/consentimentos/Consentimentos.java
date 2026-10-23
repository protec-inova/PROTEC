package com.example.protec.consentimentos;

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
    name = "consentimentos"
)
public class Consentimentos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "funcionario_id", nullable = false)
    private UUID funcionarioId;

    @Column(name = "id_maquina", nullable = false)
    private UUID maquinaId;

    @Column(length = 20)
    private String oculos;

    @Column(length = 20)
    private String bota;

    @Column(nullable = false)
    private Instant data;


    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        data = now;
    }


    public Integer getId() {
        return id;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public UUID getMaquinaId() {
        return maquinaId;
    }

    public String getOculos() {
        return oculos;
    }

    public String getBota() {
        return bota;
    }

    public Instant getData() {
        return data;
    }

    public void setFuncionarioId(UUID funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public void setMaquinaId(UUID maquinaId) {
        this.maquinaId = maquinaId;
    }

    public void setOculos(String oculos) {
        this.oculos = oculos;
    }

    public void setBota(String bota) {
        this.bota = bota;
    }
}