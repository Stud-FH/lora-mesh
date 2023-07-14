package com.example.lorameshapi.node;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/pce")
public class PceController {
    
    private final ConfigService configService;
    private final NodeService nodeService;

    @PostMapping
    public Config heartbeat(@RequestBody Node node) {
        nodeService.put(node);
        return configService.get();
    }

    @PostMapping("/node-id")
    public int allocateNodeId(
            @RequestParam int serialId,
            @RequestParam int mediator
    ) {
        return nodeService.allocateNodeId(serialId);
        // todo include mediator
    }

    // todo feed

    // todo correspondence
}
