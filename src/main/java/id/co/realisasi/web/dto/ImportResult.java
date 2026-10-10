package id.co.realisasi.web.dto;

import java.util.List;

public record ImportResult(int totalBaris, int dibuat, int diperbarui, List<String> errors) {
}
