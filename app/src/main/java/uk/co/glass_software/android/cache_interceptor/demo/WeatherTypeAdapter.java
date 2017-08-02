package uk.co.glass_software.android.cache_interceptor.demo;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.List;

class WeatherTypeAdapter extends TypeAdapter<List<Weather>> {
    @Override
    public void write(JsonWriter jsonWriter, List<Weather> weathers) throws IOException {
    
    }
    
    @Override
    public List<Weather> read(JsonReader jsonReader) throws IOException {
        return null;
    }
}
