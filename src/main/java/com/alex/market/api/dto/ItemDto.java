package com.alex.market.api.dto;

import jakarta.persistence.Column;

public record ItemDto(Long id, String title, String description, String imgPath,Long price, Integer count) {
}


