package com.bidhub.auction.messaging;

public final class RoutingKeys {

    public static final String AUCTION_BUY_NOW_EXECUTED = "auction.buy-now-executed";
    public static final String AUCTION_SOLD = "auction.sold";
    public static final String PAYMENT_ESCROW_HELD = "payment.escrow-held";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String DELIVERY_JOB_CREATED = "delivery.job-created";

    private RoutingKeys() {}
}
