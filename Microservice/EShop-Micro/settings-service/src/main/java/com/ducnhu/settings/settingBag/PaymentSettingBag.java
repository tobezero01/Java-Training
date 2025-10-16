package com.ducnhu.settings.settingBag;


import com.ducnhu.settings.entity.Setting;
import com.ducnhu.settings.entity.SettingBag;

import java.util.List;

public class PaymentSettingBag extends SettingBag {
    public PaymentSettingBag(List<Setting> listSettings) {
        super(listSettings);
    }

    public String getBaseUrl() {
        return super.getValue("PAYPAL_API_BASE_URL");
    }

    public String getClientId () {
        return super.getValue("PAYPAL_API_CLIENT_ID");
    }

    public String getClientSecret() {
        return super.getValue("PAYPAL_API_CLIENT_SECRET");
    }
}
