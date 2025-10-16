package com.ducnhu.settings.service;

import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.settings.entity.Currency;
import com.ducnhu.settings.entity.Setting;
import com.ducnhu.settings.entity.SettingCategory;
import com.ducnhu.settings.repository.CurrencyRepository;
import com.ducnhu.settings.repository.SettingRepository;
import com.ducnhu.settings.settingBag.CurrencySettingBag;
import com.ducnhu.settings.settingBag.EmailSettingBag;
import com.ducnhu.settings.settingBag.PaymentSettingBag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;

    private final CurrencyRepository currencyRepository;
    private final RedisCacheService redisCacheService;


    @Override
    public List<Setting> getGeneralSettings() {
        return settingRepository.findByTwoCategories(SettingCategory.GENERAL, SettingCategory.CURRENCY);
    }

    @Override
    public void saveAll(Iterable<Setting> settings) {

        settingRepository.saveAll(settings);
        redisCacheService.evictByPrefix("eshop:v1:setting");
    }

    @Override
    public EmailSettingBag getEmailSettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        settings.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new EmailSettingBag(settings);
    }

    @Override
    public CurrencySettingBag getCurrencySettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.CURRENCY);
        return new CurrencySettingBag(settings);
    }

    @Override
    public PaymentSettingBag getPaymentSettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.PAYMENT);
        return new PaymentSettingBag(settings);
    }

    @Override
    public String getCurrencyCode() {
        Setting setting = settingRepository.findByKey("CURRENCY_ID");
        Integer currencyId = Integer.parseInt(setting.getValue());
        Currency currency = currencyRepository.findById(currencyId).get();
        return currency.getCode();
    }
}
