package Tutorial7_8.Store.dto.user;

import Tutorial7_8.Common.enums.UserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserLoginResponse {
    String username;
    String email;
    String jwtToken;
    UserType type;
}
