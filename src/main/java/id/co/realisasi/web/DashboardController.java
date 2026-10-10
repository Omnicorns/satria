package id.co.realisasi.web;

import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.service.DashboardService;
import id.co.realisasi.web.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    /** Grafik Planning · Pagu · Realisasi, Distribusi Capaian, Progress Mata Anggaran. */
    @GetMapping("/monitoring-rka")
    public DashboardDto.MonitoringRka monitoringRka(@RequestParam(required = false) Long companyId,
                                                    @RequestParam(required = false) Long divisiId,
                                                    @RequestParam(required = false) Integer tahun,
                                                    @RequestParam(required = false) TipeAnggaran tipe) {
        return service.monitoringRka(companyId, divisiId, tahun, tipe);
    }

    /** Grafik Status pekerjaan, Beban kerja per PIC, Progress pekerjaan. */
    @GetMapping("/capex-opex")
    public DashboardDto.CapexOpex capexOpex(@RequestParam(required = false) Long companyId,
                                                    @RequestParam(required = false) Long divisiId,
                                            @RequestParam(required = false) Integer tahun,
                                            @RequestParam(required = false) TipeAnggaran tipe) {
        return service.capexOpex(companyId, divisiId, tahun, tipe);
    }

    /** Executive Budget Dashboard: serapan tahun X vs usulan RKAP tahun X+1. */
    @GetMapping("/group-overview")
    public DashboardDto.GroupOverview groupOverview(@RequestParam(required = false) Long companyId,
                                                    @RequestParam(required = false) Long divisiId,
                                                    @RequestParam(required = false) Integer tahun) {
        return service.groupOverview(companyId, divisiId, tahun);
    }
}
