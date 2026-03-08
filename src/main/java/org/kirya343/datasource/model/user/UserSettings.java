package org.kirya343.datasource.model.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class UserSettings {

    public UserSettings(User user) {
        this.user = user;
    }

    @Id
    private Long id;

    @OneToOne
    @MapsId
    private User user;

    @Setter
    private boolean telegramConnected = false;

    @Setter
    private boolean discordConnected = false;

    @Setter
    @Column(unique = true)
    private Long telegramId;

    @Setter
    @Column(unique = true)
    private Long discordId;
}
