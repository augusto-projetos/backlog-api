package com.augustoprojetos.backlogapi.dto.tmdb;

public record TmdbMovie(
        String title,
        String original_name,
        String name,
        String release_date,
        String first_air_date,
        String poster_path
) {
    public String getTituloReal() {
        if (title != null) return title;
        if (name != null) return name;
        return original_name;
    }

    public String getDataReal() {
        return release_date != null ? release_date : first_air_date;
    }
}