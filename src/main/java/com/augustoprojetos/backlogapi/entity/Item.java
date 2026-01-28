package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.augustoprojetos.backlogapi.entity.User;
import lombok.Data;

@Entity // Diz pro Spring: "Isso aqui é uma tabela no banco!"
@Data   // O Lombok cria Getters, Setters e toString sozinho (mágica!)
public class Item {

    @Id // Diz que esse é a Chave Primária (PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título não pode estar vazio") // Não aceita null nem "" nem " "
    private String titulo;

    @NotBlank(message = "O tipo é obrigatório")
    private String tipo;

    @NotBlank(message = "O status é obrigatório")
    private String status;

    @NotNull(message = "A nota é obrigatória")
    @Min(value = 0, message = "A nota deve ser no mínimo 0")
    @Max(value = 10, message = "A nota deve ser no máximo 10")
    private Double nota;

    private String resenha; // Esse pode ser vazio

    @Size(max = 500, message = "O link da imagem é muito longo")
    private String imagemUrl;

    @ManyToOne // Diz: "Muitos itens podem pertencer a UM usuário"
    @JoinColumn(name = "user_id") // Cria a coluna 'user_id' no banco
    private User user;
}