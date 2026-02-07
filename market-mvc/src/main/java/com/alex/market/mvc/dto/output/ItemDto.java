package com.alex.market.mvc.dto.output;

public record ItemDto(Long id, String title, String description, String imgPath, Long price, Integer count,
                      String imageAsBase64) {

    public ItemDto(Long id, String title, String description, String imgPath, Long price, Integer count) {
        this(id, title, description, imgPath, price, count, "");
    }
}


