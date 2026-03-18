package Tutorial7_8.Store.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import Tutorial7_8.Common.enums.UserType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @Id
    private Long id;

    @Size(min = 3, max = 50)
    private String username;

    @Email
    private String email;

    @Size(min = 8, max = 64)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private UserType type;
}