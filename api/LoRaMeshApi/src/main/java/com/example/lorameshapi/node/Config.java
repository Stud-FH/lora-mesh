package com.example.lorameshapi.node;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Config {

    @Id
    @Column(name = "id", nullable = false)
    Integer id = 1;

    @Column
    int frequency = 20;

    @Column
    int dataRate = 20;

    @Column
    int spreadingFactor = 20;

}
