package id.co.realisasi.web;

import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.excel.RkaExcelService;
import id.co.realisasi.service.BusinessException;
import id.co.realisasi.service.ProgramKerjaService;
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

    @GetMapping
    public List<ProgramKerjaDto.Response> list(@RequestParam(required = false) Long companyId,
                                               @RequestParam(required = false) Long divisiId,
                                               @RequestParam(required = false) Integer tahun,
                                               @RequestParam(required = false) TipeAnggaran tipe) {
        return service.list(companyId, divisiId, tahun, tipe);
    }

    @GetMapping("/{id}")
    public ProgramKerjaDto.Response get(@PathVariable Long id) {
        return service.getResponse(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramKerjaDto.Response create(@Valid @RequestBody ProgramKerjaDto.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ProgramKerjaDto.Response update(@PathVariable Long id, @Valid @RequestBody ProgramKerjaDto.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> template() {
        return file(excelService.template(), "template-rka.xlsx");
    }

    @GetMapping("/excel/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long companyId,
                                         @RequestParam(required = false) Long divisiId,
                                         @RequestParam(required = false) Integer tahun,
                                         @RequestParam(required = false) TipeAnggaran tipe) {
        String nama = "monitoring-rka" + (tahun != null ? "-" + tahun : "") + ".xlsx";
        return file(excelService.export(companyId, divisiId, tahun, tipe), nama);
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
