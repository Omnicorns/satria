package id.co.realisasi.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PekerjaanStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    @Test
    void selesaiBilaProgress100MeskipunLewatEstimasi() {
        Pekerjaan k = pekerjaan(100, TODAY.minusDays(10));
        assertThat(k.status(TODAY)).isEqualTo(StatusPekerjaan.SELESAI);
    }

    @Test
    void terlambatBilaLewatEstimasiDanBelumSelesai() {
        Pekerjaan k = pekerjaan(60, TODAY.minusDays(1));
        assertThat(k.status(TODAY)).isEqualTo(StatusPekerjaan.TERLAMBAT);
    }

    @Test
    void onTrackBilaTanpaEstimasiAtauBelumJatuhTempo() {
        assertThat(pekerjaan(0, null).status(TODAY)).isEqualTo(StatusPekerjaan.ON_TRACK);
        assertThat(pekerjaan(10, TODAY).status(TODAY)).isEqualTo(StatusPekerjaan.ON_TRACK);
    }

    private static Pekerjaan pekerjaan(int progress, LocalDate estimasi) {
        Pekerjaan k = new Pekerjaan();
        k.setProgress(progress);
        k.setEstimasiSelesai(estimasi);
        return k;
    }
}
