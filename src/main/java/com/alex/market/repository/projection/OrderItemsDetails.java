package com.alex.market.repository.projection;

public record OrderItemsDetails(Long id, String title, String description, String imgPath,Long price, Integer count) {
}
