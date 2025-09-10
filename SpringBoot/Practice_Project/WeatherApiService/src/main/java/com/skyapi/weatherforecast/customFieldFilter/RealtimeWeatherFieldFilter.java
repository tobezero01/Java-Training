package com.skyapi.weatherforecast.customFieldFilter;

import com.skyapi.weatherforecast.realtime.RealtimeWeatherDTO;

import java.util.HashMap;
import java.util.Objects;

public class RealtimeWeatherFieldFilter {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RealtimeWeatherDTO ) {
            RealtimeWeatherDTO dto = (RealtimeWeatherDTO) obj;
            return dto.getStatus() == null;
        }
        return false;
    }
}
