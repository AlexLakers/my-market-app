package com.alex.market.api.dto.output;

public record PageDto(Integer pageSize, Integer pageNumber, boolean hasPrevious, boolean hasNext) {
}
