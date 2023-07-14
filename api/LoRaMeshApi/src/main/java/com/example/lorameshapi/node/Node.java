package com.example.lorameshapi.node;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Entity
public class Node {

    @Id
    @Column(name = "serial_id", nullable = false)
    private Long serialId;

    @Column
    private int nodeId;

    @Enumerated(EnumType.STRING)
    private NodeStatus status;

    @Column
    private long lastUpdated;

    @ElementCollection
    private Map<Integer, Double> retx;
}
