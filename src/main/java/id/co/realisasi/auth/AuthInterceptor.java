package id.co.realisasi.auth;

import id.co.realisasi.domain.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.cors.CorsUtils;

/**
 * Memeriksa header {@code Authorization: Bearer <token>} untuk semua {@code /api/**} kecuali login.
 * User yang valid disimpan di atribut request {@link #USER_ATTR}.
 * Bila {@code app.auth.required=false} (dev/test), request tanpa token tetap diteruskan.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTR = "id.co.realisasi.user";

    private final AuthService authService;
    private final boolean required;

    public AuthInterceptor(AuthService authService, @Value("${app.auth.required:true}") boolean required) {
        this.authService = authService;
        this.required = required;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String token = bearer(request);
        if (token == null) {
            if (required) {
                throw new AuthException("Silakan login terlebih dahulu");
            }
            return true;
        }
        AppUser user = authService.userForToken(token);
        request.setAttribute(USER_ATTR, user);
        return true;
    }

    /** User login pada request yang sedang berjalan (null bila tanpa token saat auth tidak wajib). */
    public static AppUser currentUser() {
        return RequestContextHolder.getRequestAttributes() == null ? null
                : (AppUser) RequestContextHolder.getRequestAttributes()
                        .getAttribute(USER_ATTR, RequestAttributes.SCOPE_REQUEST);
    }

    static String bearer(HttpServletRequest request) {
        String h = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (h == null || !h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = h.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
