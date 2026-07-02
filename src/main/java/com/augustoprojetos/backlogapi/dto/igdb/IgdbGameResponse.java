package com.augustoprojetos.backlogapi.dto.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IgdbGameResponse {
    private Long id;
    private String name;
    private Cover cover;

    // 🔥 Adiciona o mapeamento do timestamp da data de lançamento
    @JsonProperty("first_release_date")
    private Long firstReleaseDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Cover getCover() { return cover; }
    public void setCover(Cover cover) { this.cover = cover; }
    public Long getFirstReleaseDate() { return firstReleaseDate; }
    public void setFirstReleaseDate(Long firstReleaseDate) { this.firstReleaseDate = firstReleaseDate; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cover {
        @JsonProperty("url")
        private String url;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
