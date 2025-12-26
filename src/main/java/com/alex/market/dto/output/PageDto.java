package com.alex.market.dto.output;

public record PageDto(Integer pageSize, Integer pageNumber, boolean hasPrevious, boolean hasNext) {
}
