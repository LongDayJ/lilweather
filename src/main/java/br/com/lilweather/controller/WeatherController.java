package br.com.lilweather.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lilweather.clima.Weather;
import br.com.lilweather.dto.WeatherListDTO;
import br.com.lilweather.repository.WeatherRepository;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/weather")
public class WeatherController {
    private static final String APIkey = "0c09765e328574bfbec932547d0f39fd";
    private static final String APIurl = "https://api.openweathermap.org/data/2.5/forecast?q=";
    private static final String APIlang = "&lang=pt_br";

    @Autowired
    private WeatherRepository repository;

    @GetMapping("/previsao")
    @Transactional
    public String previsao(@RequestParam("cidade") String cityName) throws IOException, InterruptedException {
        String nomeTratado = cityName.replace(" ", "+");
        String url = "" + APIurl + nomeTratado + APIlang + "&appid=" + APIkey;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build(); // BUILDER
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String json = response.body();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray listArray = jsonObject.getJSONArray("list");
        JSONObject cityObject = jsonObject.getJSONObject("city");

        Weather[] forecastDays = new Weather[5];

        for (int i = 0; i < listArray.length(); i = i + 8) {
            JSONObject listItem = listArray.getJSONObject(i);
            JSONArray weatherArray = listItem.getJSONArray("weather");
            JSONObject weatherObject = weatherArray.getJSONObject(0);

            Weather weatherForecast = new Weather(
                    null,
                    cityObject.getString("name"),
                    cityObject.getString("country"),
                    weatherObject.getString("main"),
                    weatherObject.getString("description"),
                    listItem.getString("dt_txt"));

            if (i == 0) {
                forecastDays[i] = weatherForecast;
                repository.save(weatherForecast);
            } else if (i != 0 &&
                    weatherForecast.getTimer().contains(forecastDays[0].getTimer().split(" ")[1])) {
                forecastDays[i / 8] = weatherForecast;
                repository.save(weatherForecast);
            }
        }

        return "(╯°□°）╯︵ ┻━┻";
    }

    @GetMapping("/list")
    public List<WeatherListDTO> listar() {
        return repository.findAll().stream().map(WeatherListDTO::new).toList();
    }

    @GetMapping("/historico")
    public Page<WeatherListDTO> historico(Pageable pageable, @RequestParam("page") int page) {
        return repository.findAll(pageable).map(WeatherListDTO::new);
    }
    
}
