package Tutorial7_8.Store.controller;

import Tutorial7_8.Store.dto.user.*;
import Tutorial7_8.Common.enums.UserType;
import Tutorial7_8.Store.service.UserService;
import Tutorial7_8.Store.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "Relative user API endpoints")
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    @Autowired
    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Create user", description = "signup a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Create successfully",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "parameter error")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserCreateRequest request) {
        log.info("createUser /demo");
        UserDTO user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> loginUser(@RequestBody UserLoginRequest request) {
        log.info("loginUser /demo");
        UserDTO user = userService.loginUser(request.getEmail(), request.getPassword());
        // set the response token
        String user_id = String.valueOf(user.getId());
        String jwt = jwtService.createToken(user_id, user.getEmail(), user.getUsername(), user.getType().toString());
        ResponseCookie cookie = jwtService.buildCookie(jwt);
        UserLoginResponse us = UserLoginResponse.builder().
                username(user.getUsername())
                .email(user.getEmail())
                .type(user.getType())
                .jwtToken(jwt)
                .build();
        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(us);
    }

    @PostMapping("/logout")
    public ResponseEntity<UserDTO> logoutUser(@RequestBody UserLoginRequest request) {
        log.info("logoutUser /demo");
        return ResponseEntity.ok(null);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(HttpServletRequest req) {
        String userId = (String) req.getAttribute("auth.userId");
        String username = (String) req.getAttribute("auth.username");
        String email = (String) req.getAttribute("auth.email");
        String type = (String) req.getAttribute("auth.type");
        log.info("who am i: userId={}, name={}, email={}, type={}", userId, username, email, type);
        return ResponseEntity.ok(UserDTO.builder()
                .username(username)
                .email(email)
                .id(Long.valueOf(userId))
                .type(UserType.valueOf(type))
                .build());
    }

    @PutMapping
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserUpdateRequest request) {
        log.info("updateUser /demo");
        UserDTO user = userService.updateUser(request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping
    public ResponseEntity<UserDTO> deleteUser(@RequestBody Long user_id) {
        log.info("deleteUser /demo");
        UserDTO user = userService.deleteUser(user_id);
        return ResponseEntity.ok(user);
    }

}
