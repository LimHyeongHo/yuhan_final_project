package com.Nbbang.backend.domain.bookreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookReportJsonCodec {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String writeStrings(List<String> values) {
        return write(values);
    }

    public String writeLongs(List<Long> values) {
        return write(values);
    }

    public List<String> readStrings(String value) {
        return read(value, new TypeReference<>() {
        });
    }

    public List<Long> readLongs(String value) {
        return read(value, new TypeReference<>() {
        });
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize book report data", e);
        }
    }

    private <T> List<T> read(String value, TypeReference<List<T>> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize book report data", e);
        }
    }
}
