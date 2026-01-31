package com.augustoprojetos.backlogapi.dto.tmdb;

import java.util.List;

public record TmdbResponse(List<TmdbMovie> results) {}