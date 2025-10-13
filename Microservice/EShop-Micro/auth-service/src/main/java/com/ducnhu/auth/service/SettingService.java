package com.ducnhu.auth.service;


import com.ducnhu.auth.entity.Setting;
import com.ducnhu.auth.setting.EmailSettingBag;
import com.ducnhu.auth.setting.PaymentSettingBag;

import java.util.List;

public interface SettingService {
    List<Setting> getGeneralSettings();
    void saveAll(Iterable<Setting> settings);
    EmailSettingBag getEmailSettings();
    PaymentSettingBag getPaymentSettings();
}
