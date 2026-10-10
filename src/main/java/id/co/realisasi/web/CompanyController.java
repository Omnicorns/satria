package id.co.realisasi.web;

import id.co.realisasi.service.CompanyService;
import id.co.realisasi.web.dto.CompanyDto;
import jakarta.validation.Valid;
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
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;

    @GetMapping
    public List<CompanyDto.Response> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CompanyDto.Response get(@PathVariable Long id) {
        return CompanyService.toResponse(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDto.Response create(@Valid @RequestBody CompanyDto.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public CompanyDto.Response update(@PathVariable Long id, @Valid @RequestBody CompanyDto.Request req) {
        return service.update(id, req);
    }
}
