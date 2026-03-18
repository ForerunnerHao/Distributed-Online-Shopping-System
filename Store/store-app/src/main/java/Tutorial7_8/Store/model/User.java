package Tutorial7_8.Store.model;

import Tutorial7_8.Common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email") // ← 唯一约束名
    }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Postgres/MySQL
    @Column(name = "id")
    private Long id;

    @Version
    private Long version;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username", nullable = false)
    private String username;

    @Enumerated(EnumType.STRING) // store the string to avoid the reorder error
    @Builder.Default
    @Column(name = "type", nullable = false, length = 32)
    private UserType type = UserType.USER;

    @Builder.Default
    @Column(name = "activated", nullable = false)
    private boolean activated = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
