package id.co.realisasi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {

    private Money() {
    }

    static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Persentase part/whole dengan 2 desimal; 0 bila whole nol. */
    static double percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return 0d;
        }
        return nz(part).multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
