package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity // Diz pro Spring: "Isso aqui é uma tabela no banco!"
@Data   // O Lombok cria Getters, Setters e toString sozinho (mágica!)
public class Item {

    @Id // Diz que esse é a Chave Primária (PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment (1, 2, 3...)
    private Long id;

    private String titulo;      // Ex: "God of War"
    private String tipo;        // Ex: "Jogo", "Filme", "Série"
    private String status;      // Ex: "Zerado", "Assistindo", "Dropado"
    private Integer nota;       // Ex: 10
    private String resenha;     // Ex: "História incrível, mas o final..."
    private String imagemUrl;   // Ex: "https://site.com/foto.jpg"

}