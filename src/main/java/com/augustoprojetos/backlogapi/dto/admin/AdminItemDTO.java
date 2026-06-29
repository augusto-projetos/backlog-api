package com.augustoprojetos.backlogapi.dto.admin;

import lombok.Data;

@Data
public class AdminItemDTO {
    private Long id;
    private String titulo;
    private String tipo;
    private String status;
    private String userLogin;
}
