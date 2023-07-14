package com.example.lorameshapi.data;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DataService {

    private final DataRepository dataRepository;

    public void persist(Data data) {
        this.dataRepository.save(data);
    }
}
