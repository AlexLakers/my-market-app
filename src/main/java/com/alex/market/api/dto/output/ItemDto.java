package com.alex.market.api.dto.output;

public record ItemDto(Long id, String title, String description, String imgPath,Long price, Integer count) {
}


