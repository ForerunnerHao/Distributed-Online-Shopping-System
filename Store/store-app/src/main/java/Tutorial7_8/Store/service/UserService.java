package Tutorial7_8.Store.service;

import Tutorial7_8.Store.dto.user.UserCreateRequest;
import Tutorial7_8.Store.dto.user.UserMapper;
import Tutorial7_8.Store.dto.user.UserDTO;
import Tutorial7_8.Store.dto.user.UserUpdateRequest;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.User;
import Tutorial7_8.Store.repository.UserRepository;
import Tutorial7_8.Store.utils.HashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final HashCode hashCode;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.userMapper = new UserMapper();
        this.hashCode = new HashCode();
    }

    @Transactional
    public UserDTO createUser(UserCreateRequest req) {
        String rawPassword = req.getPassword();
        String hashPassword = hashCode.hash(rawPassword);
        User entity = userMapper.toEntity(req, hashPassword);
        return userMapper.toDto(userRepository.save(entity));
    }

    public UserDTO loginUser(String email, String rawPassword) {
        User user = userRepository.getUsersByEmail(email);
        if (user == null)
            throw new BusinessException("LOGIN_ERROR_USER", "Email or password is not correct.", HttpStatus.BAD_REQUEST);
        if (!hashCode.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("LOGIN_ERROR", "Email or password is not correct", HttpStatus.BAD_REQUEST);
        }
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDTO updateUser(UserUpdateRequest req) {
        if (req == null || req.getId() == null) throw new IllegalArgumentException("id required");
        String hashPassword = req.getPassword();
        // check the user exist in the database
        User user = userRepository.findById(req.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Can not find user", HttpStatus.NOT_FOUND));
        if (user == null) return null;
        // update the user info
        userMapper.update(user, req, hashPassword);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDTO deleteUser(Long id) {
        // TODO: finish delete function (soft delete, set 'activated' field to 'false')
        log.info("Deleting user with id: {}", id);
        return null;
    }
}
