package com.alex.market.repository;

import com.alex.market.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;


public interface ItemRepository extends JpaRepository<Item, Long> , JpaSpecificationExecutor<Item> {

    Page<Item> findAll(Specification spec, Pageable pageable);

    boolean existsByTitle(String title);

}
