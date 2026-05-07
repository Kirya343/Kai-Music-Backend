package org.kirya343.datasource.model.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.core.config.Constants;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.enums.AuthProvider;
import org.kirya343.enums.UserStatus;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Table(name = "users")
@Getter
public class User {

    public User(String email, String name, String passwordHash, Set<Role> roles) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.roles = roles;

        this.settings = new UserSettings(this);
    }

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String openId = NanoIdUtils.randomNanoId(
        NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
        Constants.ALPHANUMERIC,
        20
    );

    @Setter
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSettings settings;
 
    @Setter
    @Column(unique = true)
    private String name;

    @Setter
    @Column(unique = true)
    private String email;

    @Setter
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;
    
    @Setter
    private String avatarUrl;

    @Setter
    private String passwordHash;

    @Column(length = 40, nullable = false, unique = true)
    private String apiKey = NanoIdUtils.randomNanoId(
        NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
        Constants.ALPHANUMERIC,
        40
    );

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_auth_providers",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "provider") // имя колонки для enum
    @Enumerated(EnumType.STRING)
    private Set<AuthProvider> providers = new HashSet<>(Set.of(AuthProvider.LOCAL));

    @ManyToOne
    private ListeningRoom listeningRoom;

    @Transient
    @Setter
    private boolean isNew;
}
