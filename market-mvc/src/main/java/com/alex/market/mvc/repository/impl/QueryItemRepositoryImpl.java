package com.alex.market.mvc.repository.impl;

import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.repository.QueryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class QueryItemRepositoryImpl implements QueryItemRepository {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Mono<Page<Item>> findAll(String search, Pageable pageable) {

        Criteria criteria = buildSearchCriteria(search);

        Mono<Long> count = r2dbcEntityTemplate.select(Item.class)
                .matching(Query.query(criteria))
                .count()
                .defaultIfEmpty(0L);

        Flux<Item> items = r2dbcEntityTemplate.select(Item.class)
                .matching(Query.query(criteria)
                        .with(pageable)).all();

        return items.collectList()
                .zipWith(count)
                .map(tuple -> new PageImpl<Item>(tuple.getT1(), pageable, tuple.getT2()));
    }

    @Override
    public Mono<Void> updateImagePathById(Long id, String imagePath) {
        return r2dbcEntityTemplate.update(Item.class)
                .matching(Query.query(Criteria.where("id").is(id)))
                .apply(Update.update("img_path", imagePath))
                .then();

    }
      /*  Criteria criteria = buildSearchCriteria(search);

        Flux<Item> content = r2dbcEntityTemplate.select(Item.class)
                .matching(Query.query(criteria).with(pageable))
                .all();

        Mono<Long> count = r2dbcEntityTemplate.select(Item.class)
                .matching(Query.query(criteria))
                .count();

        Mono<Integer> totalPages = count.map(c -> (int) Math.ceil((double) c / pageable.getPageSize()));

        return new PageItems(
                content,
                count,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalPages
        );
    }*/

    private Criteria buildSearchCriteria(String search) {
        if (!StringUtils.hasText(search)) {
            return Criteria.empty();
        }
        String searchParam = "%" + search + "%";
        return Criteria.where(Item.Fields.title).like(searchParam)
                .or(Criteria.where(Item.Fields.description).like(searchParam));
    }
}
