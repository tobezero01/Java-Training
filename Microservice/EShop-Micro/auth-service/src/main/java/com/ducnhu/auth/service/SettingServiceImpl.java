package com.ducnhu.auth.service;

import com.ducnhu.auth.entity.Setting;
import com.ducnhu.auth.entity.SettingCategory;
import com.ducnhu.auth.repository.SettingRepository;
import com.ducnhu.auth.setting.EmailSettingBag;
import com.ducnhu.auth.setting.PaymentSettingBag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;

    @Override
    public List<Setting> getGeneralSettings() {
        return settingRepository.findByTwoCategories(SettingCategory.GENERAL, SettingCategory.CURRENCY);
    }

    @Override
    public void saveAll(Iterable<Setting> settings) {
        settingRepository.saveAll(settings);
    }

    @Override
    public EmailSettingBag getEmailSettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        settings.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new EmailSettingBag(settings);
    }


    @Override
    public PaymentSettingBag getPaymentSettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.PAYMENT);
        return new PaymentSettingBag(settings);
    }

}

