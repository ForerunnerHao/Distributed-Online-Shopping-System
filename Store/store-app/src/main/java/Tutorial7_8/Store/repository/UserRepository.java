package Tutorial7_8.Store.repository;

import Tutorial7_8.Store.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User getUsersByEmail(String email);
}
