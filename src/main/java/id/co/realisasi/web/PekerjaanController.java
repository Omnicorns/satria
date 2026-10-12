package id.co.realisasi.web;

import id.co.realisasi.auth.AuthInterceptor;
import id.co.realisasi.auth.AuthService;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.service.PekerjaanService;
import id.co.realisasi.service.ProgramKerjaService;
import id.co.realisasi.web.dto.PageResponse;
import id.co.realisasi.web.dto.PekerjaanDto;
import id.co.realisasi.web.dto.RealisasiDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pekerjaan CAPEX/OPEX dengan tab Lihat progress, Timeline dan Financial. */
@RestController
@RequestMapping("/api/pekerjaan")
@RequiredArgsConstructor
public class PekerjaanController {

    private final PekerjaanService service;
    private final ProgramKerjaService programKerjaService;
    private final AuthService auth;

    @GetMapping
    public List<PekerjaanDto.Response> list(@RequestParam(required = false) Long companyId,
                                            @RequestParam(required = false) Long divisiId,
                                            @RequestParam(required = false) Integer tahun,
                                            @RequestParam(required = false) TipeAnggaran tipe,
                                            @RequestParam(required = false) String pic,
                                            @RequestParam(required = false) Long programKerjaId) {
        return service.list(companyId, divisi(divisiId), tahun, tipe, pic, programKerjaId);
    }

    /** Tabel Daftar Pekerjaan: ?page=0&size=20&q=...; summary = jumlah per status atas seluruh data yang cocok. */
    @GetMapping(params = "page")
    public PageResponse<PekerjaanDto.Response> page(@RequestParam(required = false) Long companyId,
                                                     @RequestParam(required = false) Long divisiId,
                                                     @RequestParam(required = false) Integer tahun,
                                                     @RequestParam(required = false) TipeAnggaran tipe,
                                                     @RequestParam(required = false) String pic,
                                                     @RequestParam(required = false) Long programKerjaId,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam int page,
                                                     @RequestParam(required = false) Integer size,
                                                     @RequestParam(required = false) String sort) {
        return service.page(companyId, divisi(divisiId), tahun, tipe, pic, programKerjaId, q, page, size, sort);
    }

    /** Daftar PIC untuk dropdown filter (tidak terpengaruh halaman). */
    @GetMapping("/pic")
    public List<String> pic() {
        return service.picList();
    }

    @GetMapping("/{id}")
    public PekerjaanDto.Response get(@PathVariable Long id) {
        cek(id);
        return service.getResponse(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PekerjaanDto.Response create(@Valid @RequestBody PekerjaanDto.Request req) {
        cekProgram(req.programKerjaId());
        return service.create(req);
    }

    @PutMapping("/{id}")
    public PekerjaanDto.Response update(@PathVariable Long id, @Valid @RequestBody PekerjaanDto.Request req) {
        cek(id);
        cekProgram(req.programKerjaId());
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cek(id);
        service.delete(id);
    }

    // Progress
    @GetMapping("/{id}/progress")
    public List<PekerjaanDto.ProgressResponse> progress(@PathVariable Long id) {
        cek(id);
        return service.listProgress(id);
    }

    @PostMapping("/{id}/progress")
    @ResponseStatus(HttpStatus.CREATED)
    public PekerjaanDto.ProgressResponse addProgress(@PathVariable Long id,
                                                     @Valid @RequestBody PekerjaanDto.ProgressRequest req) {
        cek(id);
        return service.addProgress(id, req);
    }

    // Timeline
    @GetMapping("/{id}/timeline")
    public List<PekerjaanDto.MilestoneResponse> timeline(@PathVariable Long id) {
        cek(id);
        return service.listMilestone(id);
    }

    @PostMapping("/{id}/timeline")
    @ResponseStatus(HttpStatus.CREATED)
    public PekerjaanDto.MilestoneResponse addMilestone(@PathVariable Long id,
                                                       @Valid @RequestBody PekerjaanDto.MilestoneRequest req) {
        cek(id);
        return service.addMilestone(id, req);
    }

    @PutMapping("/{id}/timeline/{milestoneId}")
    public PekerjaanDto.MilestoneResponse updateMilestone(@PathVariable Long id, @PathVariable Long milestoneId,
                                                          @Valid @RequestBody PekerjaanDto.MilestoneRequest req) {
        cek(id);
        return service.updateMilestone(id, milestoneId, req);
    }

    @DeleteMapping("/{id}/timeline/{milestoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMilestone(@PathVariable Long id, @PathVariable Long milestoneId) {
        cek(id);
        service.deleteMilestone(id, milestoneId);
    }

    // Financial
    @GetMapping("/{id}/financial")
    public RealisasiDto.Financial financial(@PathVariable Long id) {
        cek(id);
        return service.financial(id);
    }

    @PostMapping("/{id}/realisasi")
    @ResponseStatus(HttpStatus.CREATED)
    public RealisasiDto.Response addRealisasi(@PathVariable Long id, @Valid @RequestBody RealisasiDto.Request req) {
        cek(id);
        return service.addRealisasi(id, req);
    }

    @DeleteMapping("/{id}/realisasi/{realisasiId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRealisasi(@PathVariable Long id, @PathVariable Long realisasiId) {
        cek(id);
        service.deleteRealisasi(id, realisasiId);
    }

    /** User non-admin hanya melihat pekerjaan divisinya sendiri, apa pun filter yang dikirim. */
    private Long divisi(Long divisiId) {
        Long terkunci = auth.divisiTerkunci(AuthInterceptor.currentUser());
        return terkunci != null ? terkunci : divisiId;
    }

    /** Tolak (403) pekerjaan divisi lain untuk user non-admin. */
    private void cek(Long id) {
        Long terkunci = auth.divisiTerkunci(AuthInterceptor.currentUser());
        if (terkunci != null) {
            ProgramKerjaService.checkDivisi(service.get(id).getProgramKerja(), terkunci);
        }
    }

    private void cekProgram(Long programKerjaId) {
        Long terkunci = auth.divisiTerkunci(AuthInterceptor.currentUser());
        if (terkunci != null) {
            ProgramKerjaService.checkDivisi(programKerjaService.get(programKerjaId), terkunci);
        }
    }
}
