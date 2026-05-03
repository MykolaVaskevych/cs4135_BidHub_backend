package com.bidhub.auction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidhub.auction.application.dto.AuctionResponse;
import com.bidhub.auction.application.service.AuctionService;
import com.bidhub.auction.domain.exception.BuyNowDownstreamException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.exception.InsufficientWalletFundsException;
import com.bidhub.auction.domain.exception.MissingShippingAddressException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionDuration;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.Money;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.BidRepository;
import com.bidhub.auction.domain.repository.ListingRepository;
import com.bidhub.auction.domain.service.BidValidationService;
import com.bidhub.auction.infrastructure.acl.AccountClient;
import com.bidhub.auction.infrastructure.acl.AddressInfo;
import com.bidhub.auction.infrastructure.acl.CatalogueClient;
import com.bidhub.auction.infrastructure.acl.DeliveryClient;
import com.bidhub.auction.infrastructure.acl.NotificationClient;
import com.bidhub.auction.infrastructure.acl.OrderClient;
import com.bidhub.auction.infrastructure.acl.PaymentClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuctionServiceBuyNowSagaTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidRepository bidRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private BidValidationService bidValidationService;
    @Mock private AccountClient accountClient;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderClient orderClient;
    @Mock private DeliveryClient deliveryClient;
    @Mock private NotificationClient notificationClient;
    @Mock private CatalogueClient catalogueClient;
    @InjectMocks private AuctionService auctionService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID TRANSACTION_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DELIVERY_JOB_ID = UUID.randomUUID();

    private Auction activeAuctionWithBuyNow() {
        return Auction.create(
                UUID.randomUUID(),
                SELLER_ID,
                Money.of(BigDecimal.valueOf(10)),
                Money.of(BigDecimal.valueOf(20)),
                Money.of(BigDecimal.valueOf(50)),
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
    }

    private AddressInfo addressFor(String city) {
        return new AddressInfo("1 Main St", city, "Dublin", "D01 ABCD");
    }

    @BeforeEach
    void stubHappyPathDefaults() {
        when(accountClient.defaultAddressOf(BUYER_ID, "buyer"))
                .thenReturn(addressFor("BuyerCity"));
        when(accountClient.defaultAddressOf(SELLER_ID, "seller"))
                .thenReturn(addressFor("SellerCity"));
        when(paymentClient.charge(eq(BUYER_ID), any(BigDecimal.class), anyString()))
                .thenReturn(new PaymentClient.ChargeResult(TRANSACTION_ID, BigDecimal.ZERO));
        when(orderClient.createOrder(any(), eq(BUYER_ID), eq(SELLER_ID), any(BigDecimal.class)))
                .thenReturn(ORDER_ID);
        when(deliveryClient.createDeliveryJob(
                        eq(ORDER_ID),
                        any(),
                        eq(SELLER_ID),
                        eq(BUYER_ID),
                        any(AddressInfo.class),
                        any(AddressInfo.class),
                        eq(TRANSACTION_ID),
                        any(BigDecimal.class)))
                .thenReturn(DELIVERY_JOB_ID);
    }

    @Test
    @DisplayName("buyNow: optimistic lock at saveAndFlush — order cancelled and refund issued")
    void buyNow_saveFails_compensates() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(auctionRepository.saveAndFlush(any()))
                .thenThrow(
                        new ObjectOptimisticLockingFailureException(
                                Auction.class, auction.getAuctionId()));

        assertThatThrownBy(() -> auctionService.buyNow(BUYER_ID, auction.getAuctionId()))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(orderClient).cancelOrder(ORDER_ID);
        verify(paymentClient).refund(eq(BUYER_ID), any(BigDecimal.class));
    }

    @Test
    @DisplayName("buyNow happy path: charge, real orderId, real delivery, then SOLD")
    void buyNow_happyPath_marksSoldAfterAllDownstreamSucceed() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(auctionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        AuctionResponse response = auctionService.buyNow(BUYER_ID, auction.getAuctionId());

        assertThat(response.status()).isEqualTo(AuctionStatus.SOLD);
        verify(paymentClient).charge(eq(BUYER_ID), any(BigDecimal.class), anyString());
        verify(orderClient).createOrder(any(), eq(BUYER_ID), eq(SELLER_ID), any(BigDecimal.class));
        verify(deliveryClient)
                .createDeliveryJob(
                        eq(ORDER_ID),
                        any(),
                        eq(SELLER_ID),
                        eq(BUYER_ID),
                        any(AddressInfo.class),
                        any(AddressInfo.class),
                        eq(TRANSACTION_ID),
                        any(BigDecimal.class));
        verify(paymentClient, never()).refund(any(), any());
        verify(orderClient, never()).cancelOrder(any());
    }

    @Test
    @DisplayName("buyNow: insufficient funds throws and never creates order or delivery")
    void buyNow_insufficientFunds_doesNotMarkSold() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(paymentClient.charge(eq(BUYER_ID), any(BigDecimal.class), anyString()))
                .thenThrow(new InsufficientWalletFundsException("Insufficient funds. Balance: 5"));

        assertThatThrownBy(() -> auctionService.buyNow(BUYER_ID, auction.getAuctionId()))
                .isInstanceOf(InsufficientWalletFundsException.class);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        verify(auctionRepository, never()).saveAndFlush(any());
        verify(orderClient, never()).createOrder(any(), any(), any(), any());
        verify(deliveryClient, never())
                .createDeliveryJob(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("buyNow: order creation fails — refund issued, no delivery, auction still ACTIVE")
    void buyNow_orderFails_refundsAndDoesNotMarkSold() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(orderClient.createOrder(any(), eq(BUYER_ID), eq(SELLER_ID), any(BigDecimal.class)))
                .thenThrow(new BuyNowDownstreamException("order-service down"));

        assertThatThrownBy(() -> auctionService.buyNow(BUYER_ID, auction.getAuctionId()))
                .isInstanceOf(BuyNowDownstreamException.class);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        verify(paymentClient).refund(eq(BUYER_ID), any(BigDecimal.class));
        verify(orderClient, never()).cancelOrder(any());
        verify(deliveryClient, never())
                .createDeliveryJob(any(), any(), any(), any(), any(), any(), any(), any());
        verify(auctionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("buyNow: delivery creation fails — order cancelled, refund issued, auction still ACTIVE")
    void buyNow_deliveryFails_cancelsOrderAndRefunds() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(deliveryClient.createDeliveryJob(
                        eq(ORDER_ID),
                        any(),
                        eq(SELLER_ID),
                        eq(BUYER_ID),
                        any(AddressInfo.class),
                        any(AddressInfo.class),
                        eq(TRANSACTION_ID),
                        any(BigDecimal.class)))
                .thenThrow(new BuyNowDownstreamException("delivery-service down"));

        assertThatThrownBy(() -> auctionService.buyNow(BUYER_ID, auction.getAuctionId()))
                .isInstanceOf(BuyNowDownstreamException.class);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        verify(orderClient).cancelOrder(ORDER_ID);
        verify(paymentClient).refund(eq(BUYER_ID), any(BigDecimal.class));
        verify(auctionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("buyNow: missing buyer address — fails before any charge or order")
    void buyNow_missingBuyerAddress_failsBeforeCharge() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(accountClient.defaultAddressOf(BUYER_ID, "buyer"))
                .thenThrow(new MissingShippingAddressException(BUYER_ID, "buyer"));

        assertThatThrownBy(() -> auctionService.buyNow(BUYER_ID, auction.getAuctionId()))
                .isInstanceOf(MissingShippingAddressException.class);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        verify(paymentClient, never()).charge(any(), any(), anyString());
        verify(orderClient, never()).createOrder(any(), any(), any(), any());
        verify(deliveryClient, never())
                .createDeliveryJob(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("buyNow: missing seller address — fails before any charge or order")
    void buyNow_missingSellerAddress_failsBeforeCharge() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(accountClient.defaultAddressOf(SELLER_ID, "seller"))
                .thenThrow(new MissingShippingAddressException(SELLER_ID, "seller"));

        assertThatThrownBy(() -> auctionService.buyNow(BUYER_ID, auction.getAuctionId()))
                .isInstanceOf(MissingShippingAddressException.class);

        verify(paymentClient, never()).charge(any(), any(), anyString());
    }

    @Test
    @DisplayName("buyNow: real orderId is used, never auctionId-as-orderId")
    void buyNow_usesRealOrderId() {
        Auction auction = activeAuctionWithBuyNow();
        UUID auctionId = auction.getAuctionId();
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        auctionService.buyNow(BUYER_ID, auctionId);

        verify(deliveryClient, times(1))
                .createDeliveryJob(
                        eq(ORDER_ID),
                        eq(auctionId),
                        eq(SELLER_ID),
                        eq(BUYER_ID),
                        any(AddressInfo.class),
                        any(AddressInfo.class),
                        eq(TRANSACTION_ID),
                        any(BigDecimal.class));
    }

    @Test
    @DisplayName("buyNow: real paymentTransactionId is used, never random UUID")
    void buyNow_usesRealPaymentReference() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));
        when(auctionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        auctionService.buyNow(BUYER_ID, auction.getAuctionId());

        verify(deliveryClient)
                .createDeliveryJob(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(AddressInfo.class),
                        any(AddressInfo.class),
                        eq(TRANSACTION_ID),
                        any(BigDecimal.class));
    }

    @Test
    @DisplayName("buyNow: seller buying own auction is rejected before any external call")
    void buyNow_sellerCannotBuyOwn() {
        Auction auction = activeAuctionWithBuyNow();
        when(auctionRepository.findById(auction.getAuctionId())).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.buyNow(SELLER_ID, auction.getAuctionId()))
                .isInstanceOf(IllegalAuctionStateException.class);

        verify(accountClient, never()).defaultAddressOf(any(), anyString());
        verify(paymentClient, never()).charge(any(), any(), anyString());
    }
}
