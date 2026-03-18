package Tutorial7_8.Store.utils;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashCode {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public String hash(String raw) {
        return encoder.encode(raw); // store user's password to database
    }

    public boolean matches(String raw, String hashed) {
        return encoder.matches(raw, hashed); // login verification
    }
}
