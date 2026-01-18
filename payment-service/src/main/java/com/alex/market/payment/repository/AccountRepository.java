package com.alex.market.payment.repository;

import com.alex.market.payment.model.Account;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends ReactiveCrudRepository<Account, Long>, QueryAccountRepository {

}
