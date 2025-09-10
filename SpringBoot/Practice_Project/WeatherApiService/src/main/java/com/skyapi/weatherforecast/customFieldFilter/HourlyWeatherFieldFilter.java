package com.skyapi.weatherforecast.customFieldFilter;


import com.skyapi.weatherforecast.hourly.HourlyWeatherDTO;

import java.util.HashMap;
import java.util.Objects;

public class HourlyWeatherFieldFilter {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HourlyWeatherDTO) {
            HourlyWeatherDTO dto = (HourlyWeatherDTO) obj;
            return dto.getStatus() == null && (Integer) dto.getTemperature() == null;
        }
        return false;
    }
}
