package id.co.realisasi.auth;

import id.co.realisasi.domain.AppUser;
import id.co.realisasi.domain.LdapDepartemen;
import id.co.realisasi.domain.SumberUser;
import id.co.realisasi.repository.AppUserRepository;
import id.co.realisasi.service.BusinessException;
import id.co.realisasi.web.dto.LdapDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sync seluruh user aktif AD ke tabel {@code app_user} dan departemennya ke {@code ldap_departemen}.
 * User yang belum pernah login ikut dibuat (last_login_at kosong). Flag {@code aktif} SATRIA tidak diubah.
 * Berjalan manual (POST /api/ldap/sync) dan terjadwal bila {@code ldap.sync-cron} diisi.
 */
@Slf4j
@Service
public class LdapSyncService {

    private final DirectoryAuthenticator directory;
    private final AppUserRepository users;
    private final LdapDepartemenService departemen;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final ReentrantLock running = new ReentrantLock();

    public LdapSyncService(DirectoryAuthenticator directory, AppUserRepository users,
                           LdapDepartemenService departemen, PlatformTransactionManager txManager, Clock clock) {
        this.directory = directory;
        this.users = users;
        this.departemen = departemen;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    @Scheduled(cron = "${ldap.sync-cron:-}", zone = "${app.timezone:Asia/Jakarta}")
    public void scheduledSync() {
        try {
            LdapDto.SyncResult r = sync();
            log.info("Sync LDAP terjadwal selesai: {}", r);
        } catch (RuntimeException e) {
            log.error("Sync LDAP terjadwal gagal: {}", e.getMessage());
        }
    }

    public LdapDto.SyncResult sync() {
        if (!running.tryLock()) {
            throw new BusinessException("Sync LDAP sedang berjalan, coba lagi sebentar");
        }
        try {
            // Baca AD di luar transaksi agar koneksi DB tidak tertahan.
            List<DirectoryUser> fromAd = directory.listUsers();
            return tx.execute(status -> save(fromAd));
        } finally {
            running.unlock();
        }
    }

    private LdapDto.SyncResult save(List<DirectoryUser> fromAd) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<String, LdapDepartemen> cache = new HashMap<>();
        Map<String, AppUser> existing = new HashMap<>();
        for (AppUser u : users.findAll()) {
            existing.put(u.getUsername(), u);
        }
        Set<String> seen = new HashSet<>();
        int baru = 0;
        int diperbarui = 0;
        int departemenSebelum = (int) departemen.count();
        for (DirectoryUser du : fromAd) {
            if (du.username() == null || du.username().isBlank()) {
                continue;
            }
            String key = du.username().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            AppUser u = existing.get(key);
            if (u == null) {
                u = new AppUser();
                u.setUsername(key);
                u.setSumber(directory.sumber());
                baru++;
            } else {
                diperbarui++;
            }
            u.setNama(du.nama());
            u.setEmail(du.email());
            u.setDepartemen(du.departemen());
            u.setJabatan(du.jabatan());
            u.setUserPrincipalName(du.userPrincipalName());
            u.setDistinguishedName(du.distinguishedName());
            u.setLdapDepartemen(departemen.resolve(du.departemen(), cache));
            u.setLastSyncAt(now);
            users.save(u);
        }
        long tidakAda = existing.values().stream()
                .filter(u -> u.getSumber() == SumberUser.LDAP && !seen.contains(u.getUsername()))
                .count();
        return new LdapDto.SyncResult(seen.size(), baru, diperbarui, cache.size(),
                (int) departemen.count() - departemenSebelum, (int) tidakAda, now);
    }
}
