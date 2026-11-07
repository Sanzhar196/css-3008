package org.narxoz.handler;

import jakarta.validation.Valid;
import org.narxoz.config.RequestService;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-requests")
public class RequestController {
    private final RequestService requests;

    public RequestController(RequestService requests) {
        this.requests = requests;
    }

    @PostMapping
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody CreateRequest body) {
        RequestView created = requests.register(RequestId.newId(), body.businessKey(), body.title());
        URI location = URI.create("/api/v1/purchase-requests/" + created.id().value());
        return ResponseEntity.created(location).body(RequestResponse.from(created));
    }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable UUID id) {
        return RequestResponse.from(requests.require(new RequestId(id.toString())));
    }
}
