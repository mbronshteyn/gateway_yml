package com.guidedchoice.gateway.hystrix;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HystrixController {
    // Configured in YML config
    @RequestMapping("/fallback")
    public Mono<String> fallback() {
        return Mono.just("fallback\n");
    }
}
