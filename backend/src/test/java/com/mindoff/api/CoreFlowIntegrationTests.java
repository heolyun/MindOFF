package com.mindoff.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindoff.api.domain.BillingCycle;
import com.mindoff.api.domain.FridgeItem;
import com.mindoff.api.domain.HouseholdItem;
import com.mindoff.api.domain.HouseholdItemStatus;
import com.mindoff.api.domain.NeedListStatus;
import com.mindoff.api.domain.Subscription;
import com.mindoff.api.repository.HouseholdMemberRepository;
import com.mindoff.api.repository.FridgeItemRepository;
import com.mindoff.api.repository.HouseholdItemRepository;
import com.mindoff.api.repository.NeedListItemRepository;
import com.mindoff.api.repository.SubscriptionRepository;
import com.mindoff.api.service.BootstrapService;
import com.mindoff.api.service.HomeService;
import com.mindoff.api.service.HouseholdInvitationService;
import com.mindoff.api.service.InventoryService;
import com.mindoff.api.service.NeedListService;
import com.mindoff.api.service.ReceiptService;
import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CoreFlowIntegrationTests {
    @Autowired
    private BootstrapService bootstrapService;

    @Autowired
    private FridgeItemRepository fridgeRepository;

    @Autowired
    private HouseholdItemRepository householdItemRepository;

    @Autowired
    private NeedListItemRepository needListRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private HomeService homeService;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private NeedListService needListService;

    @Autowired
    private HouseholdInvitationService invitationService;

    @Autowired
    private HouseholdMemberRepository memberRepository;

    @Test
    void bootstrapAndLifecycleFlowProducesNeedAndHomeSummary() {
        BootstrapService.BootstrapResult bootstrap = bootstrapService.bootstrap(
                "owner@mindoff.local",
                "Owner",
                "테스트 집"
        );
        var user = bootstrap.user();
        var household = bootstrap.household();

        fridgeRepository.save(new FridgeItem(
                household.getId(),
                "우유",
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        ));

        HouseholdItem tissue = householdItemRepository.save(new HouseholdItem(
                household.getId(),
                "휴지",
                LocalDate.now().minusDays(14),
                true,
                "https://example.com/tissue"
        ));
        HouseholdItem finished = inventoryService.finishHouseholdItem(tissue.getId(), user.getId(), true);

        subscriptionRepository.save(new Subscription(
                user.getId(),
                "Sample",
                BigDecimal.valueOf(12_000),
                BillingCycle.MONTHLY,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(2),
                "https://example.com/manage",
                false
        ));

        HomeService.HomeSummary summary = homeService.summarize(household.getId(), user.getId());

        assertThat(finished.getPredictedDays()).isEqualTo(14);
        assertThat(needListRepository.countByHouseholdIdAndStatus(household.getId(), NeedListStatus.NEEDED)).isEqualTo(1);
        assertThat(summary.attentionCount()).isEqualTo(2);
        assertThat(summary.needListCount()).isEqualTo(1);
        assertThat(summary.recordedFixedLivingCost()).isEqualByComparingTo("12000");
        assertThat(summary.receiptPurchaseTotal()).isEqualByComparingTo("0");
    }

    @Test
    void receiptReviewCreatesInventoryAndUpdatesPurchaseTotal() {
        BootstrapService.BootstrapResult bootstrap = bootstrapService.bootstrap(
                "receipt@mindoff.local",
                "Receipt Owner",
                "영수증 테스트 집"
        );
        var user = bootstrap.user();
        var household = bootstrap.household();

        ReceiptService.ReceiptView draft = receiptService.intake(
                household.getId(),
                user.getId(),
                "receipt.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );
        assertThat(draft.lines()).hasSize(2);

        receiptService.confirm(draft.receipt().getId(), user.getId(), new ReceiptService.ReceiptReview(
                "테스트 마트",
                LocalDate.now(),
                BigDecimal.valueOf(9_800),
                List.of(
                        new ReceiptService.ReviewedLine(
                                "우유",
                                BigDecimal.ONE,
                                BigDecimal.valueOf(2_900),
                                BigDecimal.valueOf(2_900),
                                ReceiptItemTarget.FRIDGE,
                                LocalDate.now().plusDays(7)
                        ),
                        new ReceiptService.ReviewedLine(
                                "주방세제",
                                BigDecimal.ONE,
                                BigDecimal.valueOf(6_900),
                                BigDecimal.valueOf(6_900),
                                ReceiptItemTarget.HOUSEHOLD_ITEM,
                                null
                        )
                )
        ));

        ReceiptService.ReceiptView previousMonth = receiptService.intake(
                household.getId(),
                user.getId(),
                "old-receipt.jpg",
                "image/jpeg",
                new byte[] {4, 5, 6}
        );
        receiptService.confirm(previousMonth.receipt().getId(), user.getId(), new ReceiptService.ReceiptReview(
                "지난달 마트",
                LocalDate.now().minusMonths(1),
                BigDecimal.valueOf(5_000),
                List.of()
        ));

        HomeService.HomeSummary summary = homeService.summarize(household.getId(), user.getId());
        assertThat(fridgeRepository.findAllByHouseholdIdOrderByExpiresAtAsc(household.getId()))
                .extracting(FridgeItem::getName)
                .contains("우유");
        assertThat(householdItemRepository.findAllByHouseholdIdOrderByCreatedAtDesc(household.getId()))
                .extracting(HouseholdItem::getName)
                .contains("주방세제");
        assertThat(summary.receiptPurchaseTotal()).isEqualByComparingTo("9800");
    }

    @Test
    void completingHouseholdItemNeedStartsTheNextUsageCycle() {
        BootstrapService.BootstrapResult bootstrap = bootstrapService.bootstrap(
                "repurchase@mindoff.local",
                "Repurchase Owner",
                "재구매 집"
        );
        var user = bootstrap.user();
        var household = bootstrap.household();
        HouseholdItem previous = householdItemRepository.save(new HouseholdItem(
                household.getId(),
                "주방세제",
                LocalDate.now().minusDays(20),
                true,
                "https://example.com/detergent"
        ));
        inventoryService.finishHouseholdItem(previous.getId(), user.getId(), true);
        var need = needListRepository.findAllByHouseholdIdOrderByCreatedAtDesc(household.getId()).getFirst();

        needListService.complete(need.getId(), user.getId(), LocalDate.now());

        List<HouseholdItem> items = householdItemRepository.findAllByHouseholdIdOrderByCreatedAtDesc(household.getId());
        assertThat(items).filteredOn(item -> item.getStatus() == HouseholdItemStatus.ACTIVE)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getName()).isEqualTo("주방세제");
                    assertThat(item.getPredictedDays()).isEqualTo(20);
                    assertThat(item.getPurchaseUrl()).isEqualTo("https://example.com/detergent");
                });
        assertThat(need.getStatus()).isEqualTo(NeedListStatus.PURCHASED);
    }

    @Test
    void householdInvitationRequiresAcceptanceByTheInvitedEmail() {
        BootstrapService.BootstrapResult owner = bootstrapService.bootstrap(
                "household-owner@mindoff.local",
                "Owner",
                "초대 집"
        );
        BootstrapService.BootstrapResult invited = bootstrapService.bootstrap(
                "invited@mindoff.local",
                "Invited",
                "초대 전 집"
        );

        var invitation = invitationService.create(
                owner.household().getId(),
                owner.user().getId(),
                invited.user().getEmail()
        );
        assertThat(memberRepository.findByHouseholdIdAndUserId(
                owner.household().getId(), invited.user().getId()
        )).isEmpty();

        invitationService.accept(invitation.getToken(), invited.user().getId());

        assertThat(memberRepository.findByHouseholdIdAndUserId(
                owner.household().getId(), invited.user().getId()
        )).isPresent();
        assertThat(invitation.getStatus().name()).isEqualTo("ACCEPTED");
    }

    @Test
    void cognitoBootstrapLinksOneIdentityToOneUserAndHousehold() {
        BootstrapService.BootstrapResult first = bootstrapService.bootstrapCognito(
                "cognito-subject-123",
                "member@mindoff.local",
                "Member"
        );
        BootstrapService.BootstrapResult second = bootstrapService.bootstrapCognito(
                "cognito-subject-123",
                "member@mindoff.local",
                "Updated Member"
        );

        assertThat(second.user().getId()).isEqualTo(first.user().getId());
        assertThat(second.user().getExternalSubject()).isEqualTo("cognito-subject-123");
        assertThat(second.user().getAuthProvider()).isEqualTo("COGNITO");
        assertThat(second.household().getId()).isEqualTo(first.household().getId());
    }
}
