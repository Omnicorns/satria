package id.co.realisasi.auth;

import id.co.realisasi.domain.AppUser;
import id.co.realisasi.domain.Divisi;
import id.co.realisasi.domain.LdapDepartemen;
import id.co.realisasi.repository.AppUserRepository;
import id.co.realisasi.repository.DivisiRepository;
import id.co.realisasi.repository.LdapDepartemenRepository;
import id.co.realisasi.service.NotFoundException;
import id.co.realisasi.web.dto.LdapDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Master departemen AD dan pengelompokan user per departemen. */
@Service
@RequiredArgsConstructor
public class LdapDepartemenService {

    public static final String TANPA_DEPARTEMEN = "(Tanpa departemen)";

    private final LdapDepartemenRepository departemen;
    private final AppUserRepository users;
    private final DivisiRepository divisi;
    private final Clock clock;

    /**
     * Cari/buat departemen berdasarkan nama di AD (tidak peka huruf besar/kecil). Null bila nama kosong.
     * {@code cache} menghindari query berulang saat sync ribuan user; boleh null.
     */
    LdapDepartemen resolve(String nama, Map<String, LdapDepartemen> cache) {
        if (nama == null || nama.isBlank()) {
            return null;
        }
        String clean = nama.trim().replaceAll("\\s+", " ");
        String key = clean.toLowerCase(Locale.ROOT);
        if (cache != null && cache.containsKey(key)) {
            return cache.get(key);
        }
        LdapDepartemen d = departemen.findByNamaIgnoreCase(clean).orElseGet(() -> {
            LdapDepartemen baru = new LdapDepartemen();
            baru.setNama(clean);
            return baru;
        });
        d.setLastSyncAt(LocalDateTime.now(clock));
        d = departemen.save(d);
        if (cache != null) {
            cache.put(key, d);
        }
        return d;
    }

    long count() {
        return departemen.count();
    }

    /** Semua departemen beserta username di dalamnya; user tanpa departemen dikumpulkan di grup terakhir. */
    @Transactional(readOnly = true)
    public List<LdapDto.DepartemenGroup> grouped() {
        Map<Long, List<LdapDto.UserRow>> byDept = new LinkedHashMap<>();
        List<LdapDto.UserRow> tanpa = new ArrayList<>();
        for (AppUser u : users.findAllWithDepartemenByOrderByUsernameAsc()) {
            LdapDto.UserRow row = new LdapDto.UserRow(u.getId(), u.getUsername(), u.getNama(), u.getEmail(),
                    u.getJabatan(), u.isAktif(), u.getLastLoginAt());
            if (u.getLdapDepartemen() == null) {
                tanpa.add(row);
            } else {
                byDept.computeIfAbsent(u.getLdapDepartemen().getId(), k -> new ArrayList<>()).add(row);
            }
        }
        List<LdapDto.DepartemenGroup> result = new ArrayList<>();
        for (LdapDepartemen d : departemen.findAllByOrderByNamaAsc()) {
            List<LdapDto.UserRow> list = byDept.getOrDefault(d.getId(), List.of());
            Divisi dv = d.getDivisi();
            result.add(new LdapDto.DepartemenGroup(d.getId(), d.getNama(), dv == null ? null : dv.getId(),
                    dv == null ? null : dv.getNama(), list.size(), d.getLastSyncAt(), list));
        }
        result.sort(Comparator.comparing(LdapDto.DepartemenGroup::nama, String.CASE_INSENSITIVE_ORDER));
        if (!tanpa.isEmpty()) {
            result.add(new LdapDto.DepartemenGroup(null, TANPA_DEPARTEMEN, null, null, tanpa.size(), null, tanpa));
        }
        return result;
    }

    /** Petakan departemen AD ke divisi anggaran (divisiId null = lepas pemetaan). */
    @Transactional
    public void mapDivisi(Long id, Long divisiId) {
        LdapDepartemen d = departemen.findById(id)
                .orElseThrow(() -> new NotFoundException("Departemen LDAP", id));
        d.setDivisi(divisiId == null ? null : divisi.findById(divisiId)
                .orElseThrow(() -> new NotFoundException("Divisi", divisiId)));
    }
}
