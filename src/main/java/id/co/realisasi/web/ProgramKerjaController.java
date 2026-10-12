package id.co.realisasi.web;

import id.co.realisasi.auth.AuthInterceptor;
import id.co.realisasi.auth.AuthService;
import id.co.realisasi.domain.AppUser;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.excel.RkaExcelService;
import id.co.realisasi.service.BusinessException;
import id.co.realisasi.service.ProgramKerjaService;
import id.co.realisasi.web.dto.PageResponse;
import id.co.realisasi.web.dto.ImportResult;
import id.co.realisasi.web.dto.ProgramKerjaDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Monitoring RKA: program kerja per mata anggaran, termasuk template/import/export Excel. */
@RestController
@RequestMapping("/api/rka")
@RequiredArgsConstructor
public class ProgramKerjaController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProgramKerjaService service;
    private final RkaExcelService excelService;
    private final AuthService auth;

    @GetMapping
    public List<ProgramKerjaDto.Response> list(@RequestParam(required = false) Long companyId,
                                               @RequestParam(required = false) Long divisiId,
                                               @RequestParam(required = false) Integer tahun,
                                               @RequestParam(required = false) TipeAnggaran tipe,
                                               @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        return service.list(companyId, divisi(user, divisiId), tahun, tipe);
    }

    /** Tabel Daftar Program Kerja: ?page=0&size=20&q=...; summary = total KPI atas seluruh data yang cocok. */
    @GetMapping(params = "page")
    public PageResponse<ProgramKerjaDto.Response> page(@RequestParam(required = false) Long companyId,
                                                        @RequestParam(required = false) Long divisiId,
                                                        @RequestParam(required = false) Integer tahun,
                                                        @RequestParam(required = false) TipeAnggaran tipe,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam int page,
                                                        @RequestParam(required = false) Integer size,
                                                        @RequestParam(required = false) String sort,
                                                        @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        return service.page(companyId, divisi(user, divisiId), tahun, tipe, q, page, size, sort);
    }

    @GetMapping("/{id}")
    public ProgramKerjaDto.Response get(@PathVariable Long id, @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        ProgramKerjaService.checkDivisi(service.get(id), auth.divisiTerkunci(user));
        return service.getResponse(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramKerjaDto.Response create(@Valid @RequestBody ProgramKerjaDto.Request req, @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        return service.create(req, auth.divisiTerkunci(user));
    }

    @PutMapping("/{id}")
    public ProgramKerjaDto.Response update(@PathVariable Long id, @Valid @RequestBody ProgramKerjaDto.Request req,
                                           @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        return service.update(id, req, auth.divisiTerkunci(user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        service.delete(id, auth.divisiTerkunci(user));
    }

    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> template() {
        return file(excelService.template(), "template-rka.xlsx");
    }

    @GetMapping("/excel/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long companyId,
                                         @RequestParam(required = false) Long divisiId,
                                         @RequestParam(required = false) Integer tahun,
                                         @RequestParam(required = false) TipeAnggaran tipe,
                                         @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) {
        String nama = "monitoring-rka" + (tahun != null ? "-" + tahun : "") + ".xlsx";
        return file(excelService.export(companyId, divisi(user, divisiId), tahun, tipe), nama);
    }

    @PostMapping(value = "/excel/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importExcel(
            @RequestPart("file") MultipartFile file,
            @RequestAttribute(name = AuthInterceptor.USER_ATTR, required = false) AppUser user) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("File kosong");
        }
        ImportResult result = excelService.importExcel(file.getInputStream(), auth.divisiTerkunci(user));
        HttpStatus status = result.errors().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(result);
    }

    /** User non-admin selalu dibatasi ke divisinya sendiri, apa pun filter yang dikirim. */
    private Long divisi(AppUser user, Long divisiId) {
        Long terkunci = auth.divisiTerkunci(user);
        return terkunci != null ? terkunci : divisiId;
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
