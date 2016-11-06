package com.borisjerev.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by user on 20/06/2016.
 */
public class Mapper {
    private static ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ObjectMapper getInstance() {
        return MAPPER;
    }

    public static String string(Object data) {
        try {
            return getInstance().writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T objectOrThrow(String data, Class<T> type) throws IOException {
        return getInstance().readValue(data, type);
    }
}
