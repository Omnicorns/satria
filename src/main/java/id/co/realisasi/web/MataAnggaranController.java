package id.co.realisasi.web;

import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.service.MataAnggaranService;
import id.co.realisasi.web.dto.MataAnggaranDto;
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

/** Master mata anggaran (COA); dipakai dropdown "Tipe" dan auto-isi No COA / nama pada form RKA. */
@RestController
@RequestMapping("/api/mata-anggaran")
@RequiredArgsConstructor
public class MataAnggaranController {

    private final MataAnggaranService service;

    @GetMapping
    public List<MataAnggaranDto.Response> list(@RequestParam(required = false) TipeAnggaran tipe) {
        return service.findAll(tipe);
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
}
