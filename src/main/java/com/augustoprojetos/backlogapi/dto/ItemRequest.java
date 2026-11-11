package com.augustoprojetos.backlogapi.dto;

import com.augustoprojetos.backlogapi.entity.Item;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
 * Corpo aceito em POST /itens e PUT /itens/{id}.
 *
 * Existe para NÃO expor a entidade {@link Item} como contrato de entrada: campos como
 * {@code id}, {@code user} e {@code clientRequestId} nunca vêm do cliente.
 */
public record ItemRequest(

        @NotBlank(message = "O título não pode estar vazio")
        String titulo,

        @NotBlank(message = "O tipo é obrigatório")
        String tipo,

        @NotBlank(message = "O status é obrigatório")
        String status,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 0, message = "A nota deve ser no mínimo 0")
        @Max(value = 10, message = "A nota deve ser no máximo 10")
        Double nota,

        String resenha,

        @Size(max = 500, message = "O link da imagem é muito longo")
        String imagemUrl,

        @Min(value = 0, message = "A duração não pode ser negativa")
        Integer duracaoMinutos,

        @Min(value = 0, message = "Os minutos jogados não podem ser negativos")
        Integer minutosJogados,

        @Min(value = 0, message = "A temporada não pode ser negativa")
        Integer temporadaAtual,

        @Min(value = 0, message = "O episódio não pode ser negativo")
        Integer episodioAtual,

        @Min(value = 0, message = "A duração não pode ser negativa")
        Integer duracaoTotalMinutos
) {

    /* Cria uma entidade nova (sem id, sem dono): quem chama define usuário e chave de idempotência. */
    public Item paraNovaEntidade() {
        Item item = new Item();
        aplicarEm(item);
        return item;
    }

    /* Copia todos os campos editáveis para a entidade (PUT substitui o item inteiro, inclusive com null). */
    public void aplicarEm(Item item) {
        item.setTitulo(titulo);
        item.setTipo(tipo);
        item.setStatus(status);
        item.setNota(nota);
        item.setResenha(resenha);
        item.setImagemUrl(imagemUrl);
        item.setDuracaoMinutos(duracaoMinutos);
        item.setMinutosJogados(minutosJogados);
        item.setTemporadaAtual(temporadaAtual);
        item.setEpisodioAtual(episodioAtual);
        item.setDuracaoTotalMinutos(duracaoTotalMinutos);
    }
}
