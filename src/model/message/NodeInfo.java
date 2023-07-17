package model.message;

import model.NodeStatus;

import java.util.Map;

public record NodeInfo(Long serialId, NodeStatus status, int nodeId, Map<Integer, Double> retx) {
}
