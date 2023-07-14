package com.example.lorameshapi.data;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/data")
public class DataController {

    private final DataService dataService;

    @GetMapping
    public boolean ping() {
        return true;
    }

    @PostMapping()
    public void post(@RequestBody Data data) {
        dataService.persist(data);
    }
}
