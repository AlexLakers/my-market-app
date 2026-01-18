package com.alex.market.payment.exception;

import lombok.Builder;

public record ErrorResponse (int status, String message){

}
