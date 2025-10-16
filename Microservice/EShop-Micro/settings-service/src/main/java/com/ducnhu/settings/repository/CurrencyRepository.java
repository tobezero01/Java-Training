package com.ducnhu.settings.repository;

import com.ducnhu.settings.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
}
