package id.co.realisasi.web;

import id.co.realisasi.auth.AuthException;
import id.co.realisasi.auth.AuthInterceptor;
import id.co.realisasi.auth.AuthService;
import id.co.realisasi.domain.AppUser;
import id.co.realisasi.web.dto.AuthDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService service;

    /** Login dengan username + password Active Directory. Token dipakai sebagai header Authorization: Bearer. */
    @PostMapping("/auth/login")
    public AuthDto.LoginResponse login(@Valid @RequestBody AuthDto.LoginRequest req, HttpServletRequest http) {
        return service.login(req.username(), req.password(), http.getRemoteAddr());
    }

    @GetMapping("/auth/me")
    public AuthDto.UserResponse me(@RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        if (user == null) {
            throw new AuthException("Silakan login terlebih dahulu");
        }
        return AuthService.toResponse(user);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest http) {
        String h = http.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            service.logout(h.substring(7).trim());
        }
    }

    /** User yang pernah login (data tersinkron dari AD). */
    @GetMapping("/users")
    public List<AuthDto.UserResponse> users() {
        return service.listUsers();
    }
}
