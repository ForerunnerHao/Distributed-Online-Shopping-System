package Tutorial7_8.Store.dto.user;

import Tutorial7_8.Store.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserMapper {
    public UserDTO toDto(User u) {
        if (u == null) return null;
        return UserDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .type(u.getType())
                .build();
    }

    public List<UserDTO> toDto(List<User> users) {
        if (users == null || users.isEmpty()) return List.of();
        List<UserDTO> userResponseDTOList = new ArrayList<>();
        for (User u : users) {
            userResponseDTOList.add(toDto(u));
        }
        return userResponseDTOList;
    }

    public User toEntity(UserCreateRequest req, String encodedPassword) {
        if (req == null) return null;
        return User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(encodedPassword)
                .type(req.getType())
                .build();
    }

    public void update(User user, UserUpdateRequest req, String encodedPassword) {
        if (user == null || req == null) return;
        if (req.getUsername() != null) user.setUsername(req.getUsername());
        if (req.getEmail() != null)    user.setEmail(req.getEmail());
        if (req.getType() != null)     user.setType(req.getType());
        if (encodedPassword != null && !encodedPassword.isBlank()) user.setPassword(encodedPassword);
    }

}
