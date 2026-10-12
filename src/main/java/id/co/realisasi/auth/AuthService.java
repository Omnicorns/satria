package id.co.realisasi.auth;

import id.co.realisasi.domain.AppUser;
import id.co.realisasi.domain.Divisi;
import id.co.realisasi.domain.UserSession;
import id.co.realisasi.repository.AppUserRepository;
import id.co.realisasi.repository.UserSessionRepository;
import id.co.realisasi.web.dto.AuthDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DirectoryAuthenticator directory;
    private final AppUserRepository users;
    private final LdapDepartemenService departemen;
    private final UserSessionRepository sessions;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final long sessionHours;
    private final Set<String> adminUsers;

    public AuthService(DirectoryAuthenticator directory, AppUserRepository users, LdapDepartemenService departemen,
                       UserSessionRepository sessions,
                       PlatformTransactionManager txManager, Clock clock,
                       @Value("${app.auth.session-hours:12}") long sessionHours,
                       @Value("${app.auth.admin-users:}") List<String> adminUsers) {
        this.directory = directory;
        this.users = users;
        this.departemen = departemen;
        this.sessions = sessions;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
        this.sessionHours = sessionHours;
        this.adminUsers = adminUsers.stream().map(AuthService::normalize).filter(u -> !u.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Cek username + password ke AD, simpan/perbarui data user di tabel {@code app_user}, lalu terbitkan token.
     * Panggilan ke AD dilakukan di luar transaksi agar koneksi DB tidak tertahan saat AD lambat.
     */
    public AuthDto.LoginResponse login(String rawUsername, String password, String ipAddress) {
        String username = normalize(rawUsername);
        if (username.isEmpty()) {
            throw new AuthException("Username atau password salah");
        }
        DirectoryUser du = directory.authenticate(username, password);

        return tx.execute(status -> {
            LocalDateTime now = LocalDateTime.now(clock);
            String key = du.username() != null ? du.username().toLowerCase(Locale.ROOT) : username;
            AppUser user = users.findByUsername(key).orElseGet(AppUser::new);
            if (user.getId() != null && !user.isAktif()) {
                throw new AuthException("Akun Anda dinonaktifkan di SATRIA. Hubungi admin anggaran.");
            }
            user.setUsername(key);
            user.setNama(du.nama());
            user.setEmail(du.email());
            user.setDepartemen(du.departemen());
            user.setJabatan(du.jabatan());
            user.setLdapDepartemen(departemen.resolve(du.departemen(), null));
            user.setUserPrincipalName(du.userPrincipalName());
            user.setDistinguishedName(du.distinguishedName());
            user.setSumber(directory.sumber());
            user.setLastLoginAt(now);
            user = users.save(user);

            sessions.deleteExpired(now);
            String token = newToken();
            UserSession s = new UserSession();
            s.setUser(user);
            s.setTokenHash(hash(token));
            s.setExpiresAt(now.plusHours(sessionHours));
            s.setIpAddress(ipAddress);
            sessions.save(s);

            log.info("Login berhasil: {} ({})", key, directory.sumber());
            return new AuthDto.LoginResponse(token, "Bearer", s.getExpiresAt(), toResponse(user));
        });
    }

    @Transactional(readOnly = true)
    public AppUser userForToken(String token) {
        UserSession s = sessions.findByTokenHash(hash(token))
                .orElseThrow(() -> new AuthException("Sesi tidak valid, silakan login kembali"));
        if (s.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new AuthException("Sesi sudah berakhir, silakan login kembali");
        }
        if (!s.getUser().isAktif()) {
            throw new AuthException("Akun Anda dinonaktifkan di SATRIA. Hubungi admin anggaran.");
        }
        return s.getUser();
    }

    @Transactional
    public void logout(String token) {
        sessions.deleteByTokenHash(hash(token));
    }

    @Transactional(readOnly = true)
    public List<AuthDto.UserResponse> listUsers() {
        return users.findAllWithDepartemenByOrderByUsernameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AuthDto.UserResponse me(AppUser user) {
        return toResponse(withDivisi(user));
    }

    /** Admin anggaran ({@code app.auth.admin-users}) boleh memilih divisi mana pun. */
    public boolean isAdmin(AppUser user) {
        return adminUsers.contains(user.getUsername());
    }

    /**
     * Divisi yang wajib dipakai user ini untuk program kerja: divisi hasil pemetaan departemen AD-nya.
     * Null berarti bebas memilih divisi (admin, atau tanpa login saat {@code app.auth.required=false}).
     *
     * @throws ForbiddenException bila user biasa belum punya divisi (departemen AD belum dipetakan)
     */
    @Transactional(readOnly = true)
    public Long divisiTerkunci(AppUser user) {
        if (user == null || isAdmin(user)) {
            return null;
        }
        Divisi d = divisiOf(withDivisi(user));
        if (d == null) {
            throw new ForbiddenException("Departemen Anda (" + (user.getDepartemen() == null ? "kosong"
                    : user.getDepartemen()) + ") belum dipetakan ke divisi anggaran. Hubungi admin anggaran.");
        }
        return d.getId();
    }

    private AppUser withDivisi(AppUser user) {
        return users.findWithDivisiById(user.getId()).orElse(user);
    }

    private static Divisi divisiOf(AppUser u) {
        return u.getLdapDepartemen() == null ? null : u.getLdapDepartemen().getDivisi();
    }

    /** "SARINAH\Budi", "budi@sarinah.net", " Budi " semuanya menjadi "budi". */
    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String u = raw.trim();
        int slash = u.lastIndexOf('\\');
        if (slash >= 0) {
            u = u.substring(slash + 1);
        }
        int at = u.indexOf('@');
        if (at >= 0) {
            u = u.substring(0, at);
        }
        return u.toLowerCase(Locale.ROOT);
    }

    /** Dipanggil di dalam transaksi: departemen/divisi user dimuat lazy. */
    private AuthDto.UserResponse toResponse(AppUser u) {
        Divisi d = divisiOf(u);
        return new AuthDto.UserResponse(u.getId(), u.getUsername(), u.getNama(), u.getEmail(), u.getDepartemen(),
                u.getJabatan(), u.getSumber(), u.isAktif(), u.getLastLoginAt(), isAdmin(u),
                d == null ? null : d.getId(), d == null ? null : d.getNama(),
                d == null ? null : d.getCompany().getId(), d == null ? null : d.getCompany().getNama());
    }

    private static String newToken() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
