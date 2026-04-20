package com.bidhub.delivery.web.controller;

import com.bidhub.delivery.application.dto.*;
import com.bidhub.delivery.application.service.DeliveryEscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery")
@Tag(name = "Delivery", description = "Delivery job lifecycle and escrow management")
public class DeliveryController {

    private final DeliveryEscrowService service;

    public DeliveryController(DeliveryEscrowService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create delivery job", description = "Creates a new delivery job. Called by order-service after auction win.")
    @ApiResponse(responseCode = "201", description = "Job created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public DeliveryJobResponse createJob(@Valid @RequestBody CreateDeliveryJobRequest req) {
        return service.createJob(req);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get delivery job", description = "Returns the delivery job by ID.")
    @ApiResponse(responseCode = "200", description = "Job found")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public DeliveryJobResponse getJob(@PathVariable UUID jobId) {
        return service.getJob(jobId);
    }

    @GetMapping("/me")
    @Operation(summary = "My delivery jobs", description = "Returns all delivery jobs where caller is buyer, seller, or driver.")
    @ApiResponse(responseCode = "200", description = "Jobs returned")
    public List<DeliveryJobResponse> myJobs(@RequestHeader("X-User-Id") UUID userId) {
        return service.getMyJobs(userId);
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending jobs", description = "Returns all PENDING delivery jobs available for drivers to pick up.")
    @ApiResponse(responseCode = "200", description = "Pending jobs returned")
    public List<DeliveryJobResponse> pendingJobs() {
        return service.getPendingJobs();
    }

    @PostMapping("/{jobId}/assign")
    @Operation(summary = "Assign driver", description = "Assigns a driver to a PENDING job. INV-D2.")
    @ApiResponse(responseCode = "200", description = "Driver assigned")
    @ApiResponse(responseCode = "409", description = "Invalid job state")
    public DeliveryJobResponse assignDriver(
            @PathVariable UUID jobId,
            @Valid @RequestBody AssignDriverRequest req) {
        return service.assignDriver(jobId, req.driverId());
    }

    @PostMapping("/{jobId}/collect")
    @Operation(summary = "Confirm collection", description = "Seller confirms goods collected. INV-D3.")
    @ApiResponse(responseCode = "200", description = "Collection confirmed")
    @ApiResponse(responseCode = "409", description = "Invalid job state or wrong seller")
    public DeliveryJobResponse confirmCollection(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID sellerId) {
        return service.confirmCollection(jobId, sellerId);
    }

    @PostMapping("/{jobId}/deliver")
    @Operation(summary = "Mark delivered", description = "Driver marks goods as delivered.")
    @ApiResponse(responseCode = "200", description = "Marked as delivered")
    @ApiResponse(responseCode = "409", description = "Invalid job state or wrong driver")
    public DeliveryJobResponse markDelivered(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID driverId) {
        return service.markDelivered(jobId, driverId);
    }

    @PostMapping("/{jobId}/confirm")
    @Operation(summary = "Confirm delivery", description = "Buyer confirms delivery received. Escrow released. INV-D4.")
    @ApiResponse(responseCode = "200", description = "Delivery confirmed, escrow released")
    @ApiResponse(responseCode = "409", description = "Invalid job state or wrong buyer")
    public DeliveryJobResponse confirmDelivery(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID buyerId) {
        return service.confirmDelivery(jobId, buyerId);
    }

    @PostMapping("/{jobId}/dispute")
    @Operation(summary = "Raise dispute", description = "Buyer raises a dispute. Escrow frozen. INV-D5/D6.")
    @ApiResponse(responseCode = "200", description = "Dispute raised")
    @ApiResponse(responseCode = "409", description = "Invalid job state")
    public DeliveryJobResponse raiseDispute(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID reporterId,
            @Valid @RequestBody DisputeRequest req) {
        return service.raiseDispute(jobId, reporterId, req.reason());
    }

    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel job", description = "Cancels a PENDING or ASSIGNED job.")
    @ApiResponse(responseCode = "200", description = "Job cancelled")
    @ApiResponse(responseCode = "409", description = "Cannot cancel in current state")
    public DeliveryJobResponse cancelJob(@PathVariable UUID jobId) {
        return service.cancelJob(jobId);
    }
}
