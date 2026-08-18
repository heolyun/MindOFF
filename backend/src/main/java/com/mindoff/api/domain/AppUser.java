package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "auth_provider", nullable = false, length = 40)
    private String authProvider;

    @Column(name = "external_subject", unique = true, length = 160)
    private String externalSubject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() {
    }

    public AppUser(String email, String name) {
        this.id = UUID.randomUUID();
        this.email = normalizeEmail(email);
        this.name = name.trim();
        this.authProvider = "DEV";
        this.createdAt = Instant.now();
    }

    public static AppUser cognito(String externalSubject, String email, String name) {
        AppUser user = new AppUser(email, name);
        user.authProvider = "COGNITO";
        user.externalSubject = externalSubject.trim();
        return user;
    }

    public void connectCognito(String externalSubject, String name) {
        this.externalSubject = externalSubject.trim();
        this.authProvider = "COGNITO";
        updateName(name);
    }

    public void updateName(String name) {
        this.name = name.trim();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getAuthProvider() { return authProvider; }
    public String getExternalSubject() { return externalSubject; }
    public Instant getCreatedAt() { return createdAt; }
}
