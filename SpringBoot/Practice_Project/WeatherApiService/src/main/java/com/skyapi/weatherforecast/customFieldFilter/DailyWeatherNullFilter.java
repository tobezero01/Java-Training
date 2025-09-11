package com.skyapi.weatherforecast.customFieldFilter;

import com.skyapi.weatherforecast.daily.DailyWeatherDTO;
import com.skyapi.weatherforecast.realtime.RealtimeWeatherDTO;

import java.util.HashMap;
import java.util.Objects;

public class DailyWeatherNullFilter {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DailyWeatherDTO) {
            DailyWeatherDTO dto = (DailyWeatherDTO) obj;
            return dto.getStatus() == null && (Integer) dto.getMinTemp() == null && (Integer) dto.getMaxTemp() == null;
        }
        return false;
    }
}
