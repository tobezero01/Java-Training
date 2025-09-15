package com.skyapi.weatherforecast.daily;

import com.skyapi.weatherforecast.common.DailyWeather;
import com.skyapi.weatherforecast.common.Location;
import com.skyapi.weatherforecast.exception.LocationNotFoundException;
import com.skyapi.weatherforecast.location.LocationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DailyWeatherService {
    private DailyWeatherRepository dailyWeatherRepository;
    private LocationRepository locationRepository;

    public DailyWeatherService(DailyWeatherRepository dailyWeatherRepository,
                               LocationRepository locationRepository) {
        this.dailyWeatherRepository = dailyWeatherRepository;
        this.locationRepository = locationRepository;
    }

    @Cacheable(cacheNames = "dailyWeatherByIPCache", key = "{#location.countryCode, #location.cityName}")
    public List<DailyWeather> getByLocation(Location location) {
        String countryCode = location.getCountryCode();
        String cityName = location.getCityName();

        Location locationInDB = locationRepository.findByCountryCodeAndCityName(countryCode, cityName);

        if (locationInDB == null) {
            throw new LocationNotFoundException("Location not found with the given country code and city name");
        }

        return dailyWeatherRepository.findByLocationCode(locationInDB.getCode());
    }

    @Cacheable(cacheNames = "dailyWeatherByCodeCache")
    public List<DailyWeather> getByLocationCode(String locationCode) {
        Location location = locationRepository.findByCode(locationCode);

        if (location == null) {
            throw new LocationNotFoundException("Location not found with the given code ");
        }

        return dailyWeatherRepository.findByLocationCode(locationCode);
    }

    @CachePut(cacheNames = "dailyWeatherByCodeCache", key = "#code")
    @CacheEvict(cacheNames = "dailyWeatherByIPCache", allEntries = true)
    public List<DailyWeather> updateByLocationCode(String code, List<DailyWeather> dailyWeatherInRequest) {
        Location location = locationRepository.findByCode(code);

        if (location == null) {
            throw new LocationNotFoundException("Location not found with the given code");
        }

        for (DailyWeather data : dailyWeatherInRequest) {
            data.getId().setLocation(location);
        }

        List<DailyWeather> dailyWeatherInDB = location.getListDailyWeather();
        List<DailyWeather> dailyWeatherToBeRemoved = new ArrayList<>();

        for (DailyWeather forecast : dailyWeatherInDB) {
            if (!dailyWeatherInRequest.contains(forecast) ) {
                dailyWeatherToBeRemoved.add(forecast.getShallowCopy());
            }
        }

        for (DailyWeather forecastToBeRemoved : dailyWeatherToBeRemoved) {
            dailyWeatherInDB.remove(forecastToBeRemoved);
        }

        return dailyWeatherRepository.saveAll(dailyWeatherInRequest);
    }
}
