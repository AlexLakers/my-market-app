package com.alex.market.validator;

public interface ValidMessages {
    String PRICE_POSITIVE = "The price must be positive";
    String PRICE_REQUIRED = "The price must be not null";
    String TITLE_REQUIRED = "The title must be not null";
    String DESC_REQUIRED = "The description must be not blank";
    String ID_REQUIRED = "The id must be not blank";
    String ACTION_REQUIRED = "The id must be not blank";
    String ID_POSITIVE = "The id must be positive";
    String PAGE_MIN = "The page minimum must be greater than 0";
    String SIZE_MIN = "The size must be greater than 0";
    String SIZE_MAX="The size must be less than 100";


}
