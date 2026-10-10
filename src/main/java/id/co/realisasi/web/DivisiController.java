package id.co.realisasi.web;

import id.co.realisasi.service.DivisiService;
import id.co.realisasi.web.dto.DivisiDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/** Master divisi (pemilik anggaran) per perusahaan. */
@RestController
@RequestMapping("/api/divisi")
@RequiredArgsConstructor
public class DivisiController {

    private final DivisiService service;

    @GetMapping
    public List<DivisiDto.Response> list(@RequestParam(required = false) Long companyId) {
        return service.findAll(companyId);
    }

    @GetMapping("/{id}")
    public DivisiDto.Response get(@PathVariable Long id) {
        return DivisiService.toResponse(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DivisiDto.Response create(@Valid @RequestBody DivisiDto.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public DivisiDto.Response update(@PathVariable Long id, @Valid @RequestBody DivisiDto.Request req) {
        return service.update(id, req);
    }
}
