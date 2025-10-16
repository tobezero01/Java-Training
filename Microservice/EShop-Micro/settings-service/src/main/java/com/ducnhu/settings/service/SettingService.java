package com.ducnhu.settings.service;


import com.ducnhu.settings.entity.Setting;
import com.ducnhu.settings.settingBag.CurrencySettingBag;
import com.ducnhu.settings.settingBag.EmailSettingBag;
import com.ducnhu.settings.settingBag.PaymentSettingBag;

import java.util.List;

public interface SettingService {
    List<Setting> getGeneralSettings();
    void saveAll(Iterable<Setting> settings);
    EmailSettingBag getEmailSettings();
    CurrencySettingBag getCurrencySettings();
    PaymentSettingBag getPaymentSettings();
    String getCurrencyCode();
}
