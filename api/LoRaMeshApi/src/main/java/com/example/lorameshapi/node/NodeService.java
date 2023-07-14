package com.example.lorameshapi.node;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class NodeService {

    private final NodeRepository nodeRepository;

    public void put(Node node) {
        Node entity = nodeRepository.findById(node.getSerialId()).orElseGet(Node::new);
        if (entity.getSerialId() == null) entity.setSerialId(node.getSerialId());
        entity.setStatus(node.getStatus());
        entity.setLastUpdated(System.currentTimeMillis());
        entity.getRetx().replaceAll((k,v) -> 0.0);
        entity.getRetx().putAll(node.getRetx());
        nodeRepository.save(entity);
    }

    public int allocateNodeId(long serialId) {
        Node entity = nodeRepository.findById(serialId).orElseGet(Node::new);
        if (entity.getSerialId() == null) entity.setSerialId(serialId);
        if (entity.getNodeId() > 0) return entity.getNodeId();
        int nodeId = 1;
        while (nodeRepository.existsByNodeId(nodeId)) nodeId++;
        if (nodeId > 63) throw new IllegalStateException("node ids exhausted");
        entity.setNodeId(nodeId);
        entity.setLastUpdated(System.currentTimeMillis());
        nodeRepository.save(entity);
        return nodeId;
    }
}
