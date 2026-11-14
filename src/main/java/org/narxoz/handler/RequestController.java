package org.narxoz.handler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.narxoz.config.RequestService;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @Operation(summary = "Open a purchase request")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "Created. Location is the GET url.",
                    content = @Content(schema = @Schema(implementation = RequestResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "title or businessKey blank, or title longer than 120",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409",
                    description = "business key already stored",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody CreateRequest body) {
        RequestView created = requests.register(RequestId.newId(), body.businessKey(), body.title());
        URI location = URI.create("/api/v1/purchase-requests/" + created.id().value());
        return ResponseEntity.created(location).body(RequestResponse.from(created));
    }

    @Operation(summary = "Read a purchase request by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The purchase request",
                    content = @Content(schema = @Schema(implementation = RequestResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "id is not a UUID",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404",
                    description = "no purchase request with that id",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable UUID id) {
        return RequestResponse.from(requests.require(new RequestId(id.toString())));
    }
}
