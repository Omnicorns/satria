package id.co.realisasi.web;

import id.co.realisasi.auth.LdapDepartemenService;
import id.co.realisasi.auth.LdapSyncService;
import id.co.realisasi.web.dto.LdapDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ldap")
@RequiredArgsConstructor
public class LdapController {

    private final LdapSyncService sync;
    private final LdapDepartemenService departemen;

    /** Tarik semua user aktif + departemen dari AD ke database. */
    @PostMapping("/sync")
    public LdapDto.SyncResult sync() {
        return sync.sync();
    }

    /** Departemen AD beserta daftar username di dalamnya. */
    @GetMapping("/departemen")
    public List<LdapDto.DepartemenGroup> departemen() {
        return departemen.grouped();
    }

    @PutMapping("/departemen/{id}/divisi")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mapDivisi(@PathVariable Long id, @RequestBody LdapDto.MapDivisiRequest req) {
        departemen.mapDivisi(id, req.divisiId());
    }
}
