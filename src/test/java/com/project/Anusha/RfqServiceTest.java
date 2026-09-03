package com.project.Anusha;

import com.project.Anusha.dto.*;
import com.project.Anusha.enums.NotificationType;
import com.project.Anusha.enums.RfqStatus;
import com.project.Anusha.exception.RfqException;
import com.project.Anusha.model.*;
import com.project.Anusha.repository.*;
import com.project.Anusha.service.NotificationService;
import com.project.Anusha.service.RfqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class RfqServiceTest {

    @Autowired
    private RfqService rfqService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RfqRepository rfqRepository;

    private User buyerA;
    private User buyerB;
    private User buyerC;
    private Product product501;

    @BeforeEach
    void setUp() {
        buyerA = userRepository.save(User.builder().email("buyerA@test.com").password("pass").name("Buyer A").role("ROLE_USER").build());
        buyerB = userRepository.save(User.builder().email("buyerB@test.com").password("pass").name("Buyer B").role("ROLE_USER").build());
        buyerC = userRepository.save(User.builder().email("buyerC@test.com").password("pass").name("Buyer C").role("ROLE_USER").build());

        Category category = categoryRepository.save(Category.builder().name("Grains").description("Agricultural grains").build());
        product501 = productRepository.save(Product.builder().name("Basmati Rice").description("Premium Basmati").category(category).isActive(true).build());
    }

    @Test
    @DisplayName("TEST 1: Three buyers create RFQs for same product - creates 3 independent records")
    void test1_multipleBuyersSameProduct() {
        BuyerCreateRfqRequest requestA = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .buyerMessage("Need best quotation A")
                .build();

        BuyerCreateRfqRequest requestB = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("2000 KG")
                .deliveryLocation("Guntur")
                .buyerMessage("Need best quotation B")
                .build();

        BuyerCreateRfqRequest requestC = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("3000 KG")
                .deliveryLocation("Hyderabad")
                .buyerMessage("Need best quotation C")
                .build();

        BuyerRfqResponseDto rfqA = rfqService.createRfq(buyerA, requestA);
        BuyerRfqResponseDto rfqB = rfqService.createRfq(buyerB, requestB);
        BuyerRfqResponseDto rfqC = rfqService.createRfq(buyerC, requestC);

        assertNotNull(rfqA.getRfqCode());
        assertNotNull(rfqB.getRfqCode());
        assertNotNull(rfqC.getRfqCode());

        // 3 different RFQ IDs and codes
        assertNotEquals(rfqA.getId(), rfqB.getId());
        assertNotEquals(rfqB.getId(), rfqC.getId());
        assertNotEquals(rfqA.getRfqCode(), rfqB.getRfqCode());

        // Same product ID
        assertEquals(product501.getId(), rfqA.getProduct().getId());
        assertEquals(product501.getId(), rfqB.getProduct().getId());
        assertEquals(product501.getId(), rfqC.getProduct().getId());

        // Different buyers
        Rfq entityA = rfqRepository.findById(rfqA.getId()).orElseThrow();
        Rfq entityB = rfqRepository.findById(rfqB.getId()).orElseThrow();
        Rfq entityC = rfqRepository.findById(rfqC.getId()).orElseThrow();

        assertEquals(buyerA.getId(), entityA.getBuyer().getId());
        assertEquals(buyerB.getId(), entityB.getBuyer().getId());
        assertEquals(buyerC.getId(), entityC.getBuyer().getId());
    }

    @Test
    @DisplayName("TEST 2: Buyer A requests Buyer B RFQ - throws 403 RFQ_ACCESS_DENIED")
    void test2_buyerAccessControl() {
        BuyerCreateRfqRequest reqB = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Guntur")
                .buyerMessage("Buyer B message")
                .build();

        BuyerRfqResponseDto rfqB = rfqService.createRfq(buyerB, reqB);

        RfqException exception = assertThrows(RfqException.class, () -> {
            rfqService.getBuyerRfqDetail(buyerA, rfqB.getRfqCode());
        });

        assertEquals("RFQ_ACCESS_DENIED", exception.getErrorCode());
    }

    @Test
    @DisplayName("TEST 3: Admin response received (Dummy Data) - status becomes RESPONDED, buyer notification created")
    void test3_adminRespondsToRfq() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .buyerMessage("Quotation requested")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);
        assertEquals(RfqStatus.PENDING, rfq.getStatus());

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");

        BuyerRfqResponseDto responded = rfqService.getBuyerRfqDetail(buyerA, rfq.getRfqCode());
        assertEquals(RfqStatus.RESPONDED, responded.getStatus());
        assertNotNull(responded.getResponse());
        assertEquals(62000.0, responded.getResponse().getQuotedPrice());

        // Verify Buyer Notification created
        List<NotificationResponseDto> notifications = notificationService.getBuyerNotifications(buyerA);
        assertFalse(notifications.isEmpty());
        assertEquals(NotificationType.RFQ_RESPONSE_RECEIVED, notifications.get(0).getType());
    }

    @Test
    @DisplayName("TEST 4: Buyer tries contact API before acceptance - throws 403 CONTACT_NOT_AVAILABLE")
    void test4_contactNotAvailableBeforeAcceptance() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        // Before response (PENDING)
        RfqException ex1 = assertThrows(RfqException.class, () -> {
            rfqService.getContactDetails(buyerA, rfq.getRfqCode());
        });
        assertEquals("CONTACT_NOT_AVAILABLE", ex1.getErrorCode());

        // Admin responds with dummy data (RESPONDED)
        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");

        // Still before acceptance (RESPONDED)
        RfqException ex2 = assertThrows(RfqException.class, () -> {
            rfqService.getContactDetails(buyerA, rfq.getRfqCode());
        });
        assertEquals("CONTACT_NOT_AVAILABLE", ex2.getErrorCode());
    }

    @Test
    @DisplayName("TEST 5: Buyer accepts RFQ - status becomes ACCEPTED")
    void test5_buyerAcceptsRfq() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");

        BuyerRfqResponseDto accepted = rfqService.acceptRfqResponse(buyerA, rfq.getRfqCode());
        assertEquals(RfqStatus.ACCEPTED, accepted.getStatus());
        assertTrue(accepted.isContactAvailable());
    }

    @Test
    @DisplayName("TEST 6: Buyer gets contact after acceptance - returns 200 + contact details")
    void test6_buyerGetsContactAfterAcceptance() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Manager", "9876543210", "sales@kfpcl.com");

        rfqService.acceptRfqResponse(buyerA, rfq.getRfqCode());

        ContactResponseDto contact = rfqService.getContactDetails(buyerA, rfq.getRfqCode());
        assertNotNull(contact);
        assertEquals("Sales Manager", contact.getContactName());
        assertEquals("9876543210", contact.getContactPhone());
        assertEquals("sales@kfpcl.com", contact.getContactEmail());
    }

    @Test
    @DisplayName("TEST 7: Buyer rejects RFQ - status becomes REJECTED")
    void test7_buyerRejectsRfq() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");

        BuyerRfqResponseDto rejected = rfqService.rejectRfqResponse(buyerA, rfq.getRfqCode(), "Price is too high");
        assertEquals(RfqStatus.REJECTED, rejected.getStatus());
        assertEquals("Price is too high", rejected.getRejectionReason());
    }

    @Test
    @DisplayName("TEST 8: Buyer re-raises rejected RFQ - old RFQ remains REJECTED, new RFQ is PENDING with parentRfqId")
    void test8_buyerReRaisesRfq() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");
        rfqService.rejectRfqResponse(buyerA, rfq.getRfqCode(), "Price too high");

        BuyerReRaiseRfqRequest reRaiseReq = BuyerReRaiseRfqRequest.builder()
                .quantity("1500 KG")
                .deliveryLocation("Vijayawada")
                .buyerMessage("Please provide better price")
                .build();

        BuyerRfqResponseDto newRfq = rfqService.reRaiseRfq(buyerA, rfq.getRfqCode(), reRaiseReq);

        // New RFQ
        assertEquals(RfqStatus.PENDING, newRfq.getStatus());
        assertEquals(rfq.getId(), newRfq.getParentRfqId());
        assertEquals(rfq.getRfqCode(), newRfq.getParentRfqCode());
        assertEquals("1500 KG", newRfq.getQuantity());

        // Old RFQ still REJECTED
        BuyerRfqResponseDto oldRfq = rfqService.getBuyerRfqDetail(buyerA, rfq.getRfqCode());
        assertEquals(RfqStatus.REJECTED, oldRfq.getStatus());
    }

    @Test
    @DisplayName("TEST 9: Buyer tries to accept already rejected RFQ - throws INVALID_RFQ_STATUS")
    void test9_cannotAcceptRejectedRfq() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");
        rfqService.rejectRfqResponse(buyerA, rfq.getRfqCode(), "Rejecting");

        RfqException ex = assertThrows(RfqException.class, () -> {
            rfqService.acceptRfqResponse(buyerA, rfq.getRfqCode());
        });

        assertEquals("INVALID_RFQ_STATUS", ex.getErrorCode());
    }

    @Test
    @DisplayName("TEST 10: Buyer tries to reject already accepted RFQ - throws INVALID_RFQ_STATUS")
    void test10_cannotRejectAcceptedRfq() {
        BuyerCreateRfqRequest request = BuyerCreateRfqRequest.builder()
                .productId(product501.getId())
                .quantity("1000 KG")
                .deliveryLocation("Vijayawada")
                .build();

        BuyerRfqResponseDto rfq = rfqService.createRfq(buyerA, request);

        Rfq rfqEntity = rfqRepository.findById(rfq.getId()).orElseThrow();
        rfqService.addDummyAdminResponse(rfqEntity, 62000.0, "1000 KG", "4 Days", "Transport included", "Sales Team", "9876543210", "sales@kfpcl.com");
        rfqService.acceptRfqResponse(buyerA, rfq.getRfqCode());

        RfqException ex = assertThrows(RfqException.class, () -> {
            rfqService.rejectRfqResponse(buyerA, rfq.getRfqCode(), "Rejecting after accept");
        });

        assertEquals("INVALID_RFQ_STATUS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Notification APIs: Test read-all, single read, unread count")
    void testNotifications() {
        notificationService.createNotification(buyerA, NotificationType.RFQ_RESPONSE_RECEIVED, "N1", "M1", "RFQ", "1");
        notificationService.createNotification(buyerA, NotificationType.RFQ_ACCEPTED, "N2", "M2", "RFQ", "2");
        notificationService.createNotification(buyerB, NotificationType.RFQ_RESPONSE_RECEIVED, "N3", "M3", "RFQ", "3");

        assertEquals(2, notificationService.getUnreadCount(buyerA));
        assertEquals(1, notificationService.getUnreadCount(buyerB));

        // Mark all read for buyerA
        notificationService.markAllNotificationsRead(buyerA);
        assertEquals(0, notificationService.getUnreadCount(buyerA));

        // BuyerB notifications untouched
        assertEquals(1, notificationService.getUnreadCount(buyerB));
    }
}
