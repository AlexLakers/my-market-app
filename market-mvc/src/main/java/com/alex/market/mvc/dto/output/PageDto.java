package com.alex.market.mvc.dto.output;

public record PageDto(Integer pageSize, Integer pageNumber, boolean hasPrevious, boolean hasNext) {
}
