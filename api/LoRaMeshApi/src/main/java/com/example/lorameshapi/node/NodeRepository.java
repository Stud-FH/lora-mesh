package com.example.lorameshapi.node;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<Node, Long> {

    boolean existsByNodeId(int nodeId);
}
