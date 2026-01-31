package com.alex.market.mvc.cache;

import java.util.List;

public record PageInfoCache(List<Long> itemsIds,Integer pageSize, Integer pageNumber, boolean hasPrevious, boolean hasNext) {


}
