package br.com.barbearia.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "barbeiro")
public class Barbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private boolean ativo = true;

    protected Barbeiro() {
    }

    public Barbeiro(String nome) {
        this.nome = nome;
    }

    public void renomear(String novoNome) {
        this.nome = novoNome;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
}
