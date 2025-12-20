package com.alex.market.api.dto;

public record ItemCreateDto(String title, String description, String imgPath, Long price) {
}
