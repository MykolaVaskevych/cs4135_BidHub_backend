package com.bidhub.auction.domain.model;

import java.util.UUID;

/**
 * Local cache of valid categories. Consumed from Admin context's CategoryCreated / CategoryUpdated
 * / CategoryDeleted events. Used by Listing to validate INV-L1.
 */
public record CategoryRef(UUID categoryId, String name, boolean isActive) {}
