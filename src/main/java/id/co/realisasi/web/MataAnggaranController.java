package id.co.realisasi.web;

import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.excel.MataAnggaranExcelService;
import id.co.realisasi.service.BusinessException;
import id.co.realisasi.service.MataAnggaranService;
import id.co.realisasi.web.dto.ImportResult;
import id.co.realisasi.web.dto.PageResponse;
import id.co.realisasi.web.dto.MataAnggaranDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Master mata anggaran (COA); dipakai dropdown "Tipe" dan auto-isi No COA / nama pada form RKA, termasuk template/import/export Excel. */
@RestController
@RequestMapping("/api/mata-anggaran")
@RequiredArgsConstructor
public class MataAnggaranController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final MataAnggaranService service;
    private final MataAnggaranExcelService excelService;

    @GetMapping
    public List<MataAnggaranDto.Response> list(@RequestParam(required = false) TipeAnggaran tipe) {
        return service.findAll(tipe);
    }

    /** Dipakai tabel master: ?page=0&size=20&sort=noCoa,asc&q=...; tanpa page tetap mengembalikan list penuh. */
    @GetMapping(params = "page")
    public PageResponse<MataAnggaranDto.Response> page(@RequestParam(required = false) TipeAnggaran tipe,
                                                        @RequestParam(required = false) Boolean aktif,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam int page,
                                                        @RequestParam(required = false) Integer size,
                                                        @RequestParam(required = false) String sort) {
        return service.page(tipe, aktif, q, page, size, sort);
    }

    @GetMapping("/{id}")
    public MataAnggaranDto.Response get(@PathVariable Long id) {
        return MataAnggaranService.toResponse(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MataAnggaranDto.Response create(@Valid @RequestBody MataAnggaranDto.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public MataAnggaranDto.Response update(@PathVariable Long id, @Valid @RequestBody MataAnggaranDto.Request req) {
        return service.update(id, req);
    }

    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> template() {
        return file(excelService.template(), "template-mata-anggaran.xlsx");
    }

    /** Filter sama dengan tabel master (tipe, aktif, q, sort); tanpa filter = seluruh mata anggaran. */
    @GetMapping("/excel/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) TipeAnggaran tipe,
                                         @RequestParam(required = false) Boolean aktif,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String sort) {
        return file(excelService.export(tipe, aktif, q, sort), "mata-anggaran.xlsx");
    }

    @PostMapping(value = "/excel/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importExcel(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("File kosong");
        }
        ImportResult result = excelService.importExcel(file.getInputStream());
        HttpStatus status = result.errors().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(result);
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
