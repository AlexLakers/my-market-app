package com.alex.market.api.dto;

public record PageDto(Integer pageSize, Integer pageNumber, boolean hasPrevious, boolean hasNext) {
}
