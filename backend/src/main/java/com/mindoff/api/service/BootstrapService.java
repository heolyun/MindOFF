package com.mindoff.api.service;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.Household;
import com.mindoff.api.domain.HouseholdMember;
import com.mindoff.api.domain.HouseholdRole;
import com.mindoff.api.repository.AppUserRepository;
import com.mindoff.api.repository.HouseholdMemberRepository;
import com.mindoff.api.repository.HouseholdRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapService {
    private final AppUserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository memberRepository;

    public BootstrapService(
            AppUserRepository userRepository,
            HouseholdRepository householdRepository,
            HouseholdMemberRepository memberRepository
    ) {
        this.userRepository = userRepository;
        this.householdRepository = householdRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public BootstrapResult bootstrap(String email, String name, String householdName) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    existing.updateName(name);
                    return existing;
                })
                .orElseGet(() -> new AppUser(email, name));
        userRepository.save(user);

        Household household = memberRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                .flatMap(member -> householdRepository.findById(member.getHouseholdId()))
                .orElseGet(() -> createHousehold(user.getId(), householdName));

        return new BootstrapResult(user, household);
    }

    @Transactional
    public BootstrapResult bootstrapCognito(String subject, String email, String name) {
        AppUser user = userRepository.findByExternalSubject(subject)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .map(existing -> {
                    existing.connectCognito(subject, name);
                    return existing;
                })
                .orElseGet(() -> AppUser.cognito(subject, email, name));
        userRepository.save(user);

        Household household = memberRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                .flatMap(member -> householdRepository.findById(member.getHouseholdId()))
                .orElseGet(() -> createHousehold(user.getId(), "내 집"));
        return new BootstrapResult(user, household);
    }

    private Household createHousehold(UUID ownerId, String householdName) {
        Household household = householdRepository.save(new Household(householdName, ownerId));
        memberRepository.save(new HouseholdMember(household.getId(), ownerId, HouseholdRole.OWNER));
        return household;
    }

    public record BootstrapResult(AppUser user, Household household) {
    }
}
