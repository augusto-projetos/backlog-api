package com.augustoprojetos.backlogapi.dto.admin;

import lombok.Data;

@Data
public class AdminUserDTO {
    private Long id;
    private String login;
    private String email;
    private String socialUsername;
    private boolean emailVerified;
    private boolean isPublic;
    private int totalItens;
}
