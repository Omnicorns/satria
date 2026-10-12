package id.co.realisasi.excel;

import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.service.BusinessException;
import id.co.realisasi.service.MataAnggaranService;
import id.co.realisasi.web.dto.ImportResult;
import id.co.realisasi.web.dto.MataAnggaranDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static id.co.realisasi.excel.ExcelSupport.autosize;
import static id.co.realisasi.excel.ExcelSupport.header;
import static id.co.realisasi.excel.ExcelSupport.isBlank;
import static id.co.realisasi.excel.ExcelSupport.text;
import static id.co.realisasi.excel.ExcelSupport.write;

/**
 * Template, export dan import Excel master mata anggaran (COA).
 * Kolom export sama dengan kolom import, jadi hasil export bisa diedit lalu diimpor ulang.
 * Kunci baris import: No COA (upsert).
 */
@Service
@RequiredArgsConstructor
public class MataAnggaranExcelService {

    static final String[] HEADER = {"No COA", "Nama Mata Anggaran", "Tipe", "Kategori", "Status"};
    private static final Map<String, String> KOLOM = Map.of(
            "noCoa", "No COA", "nama", "Nama Mata Anggaran", "tipe", "Tipe", "kategori", "Kategori");

    private final MataAnggaranService service;
    private final Validator validator;

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Mata Anggaran");
            header(wb, sheet, HEADER);
            row(sheet, 1, "6104050001", "Beban Sewa Mesin dan Peralatan", "OPEX", "Beban Sewa", "Aktif");
            row(sheet, 2, "1402010001", "Aset Tetap - Peralatan Kantor", "CAPEX", "Aset Tetap", "Aktif");
            autosize(sheet, HEADER.length);

            Sheet info = wb.createSheet("Petunjuk");
            String[] petunjuk = {
                    "Hapus baris contoh sebelum import.",
                    "No COA: wajib, angka 4-30 digit. Bila No COA sudah terdaftar, datanya diperbarui; bila belum, dibuat baru.",
                    "Nama Mata Anggaran: wajib, maksimal 200 karakter.",
                    "Tipe: wajib, OPEX atau CAPEX.",
                    "Kategori: opsional, maksimal 100 karakter.",
                    "Status: Aktif atau Nonaktif. Kosong = Aktif untuk data baru, tidak berubah untuk data lama.",
                    "Bila ada satu baris yang tidak valid, tidak ada data yang disimpan."
            };
            for (int i = 0; i < petunjuk.length; i++) {
                info.createRow(i).createCell(0).setCellValue(petunjuk[i]);
            }
            info.setColumnWidth(0, 110 * 256);
            return write(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] export(TipeAnggaran tipe, Boolean aktif, String q, String sort) {
        List<MataAnggaranDto.Response> list = service.search(tipe, aktif, q, sort);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Mata Anggaran");
            header(wb, sheet, HEADER);
            int r = 1;
            for (MataAnggaranDto.Response m : list) {
                row(sheet, r++, m.noCoa(), m.nama(), m.tipe().name(), m.kategori() == null ? "" : m.kategori(),
                        m.aktif() ? "Aktif" : "Nonaktif");
            }
            autosize(sheet, HEADER.length);
            return write(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Import seluruh baris; bila ada satu baris tidak valid, tidak ada yang disimpan. */
    @Transactional
    public ImportResult importExcel(InputStream in) {
        List<MataAnggaranDto.Request> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Map<String, Integer> barisPerCoa = new HashMap<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(row, HEADER.length, fmt)) {
                    continue;
                }
                int nomor = i + 1;
                parse(row, fmt, nomor, errors).ifPresent(req -> {
                    Integer sebelumnya = barisPerCoa.putIfAbsent(req.noCoa(), nomor);
                    if (sebelumnya != null) {
                        errors.add("Baris " + nomor + ": No COA " + req.noCoa() + " duplikat dengan baris " + sebelumnya);
                    } else {
                        rows.add(req);
                    }
                });
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("File bukan Excel yang valid: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            return new ImportResult(rows.size() + errors.size(), 0, 0, errors);
        }
        if (rows.isEmpty()) {
            throw new BusinessException("File tidak berisi data mata anggaran");
        }
        int dibuat = 0;
        for (MataAnggaranDto.Request req : rows) {
            if (service.upsert(req)) {
                dibuat++;
            }
        }
        return new ImportResult(rows.size(), dibuat, rows.size() - dibuat, List.of());
    }

    private Optional<MataAnggaranDto.Request> parse(Row row, DataFormatter fmt, int nomor, List<String> errors) {
        int awal = errors.size();
        String tipeStr = text(row, 2, fmt).toUpperCase(Locale.ROOT);
        TipeAnggaran tipe = null;
        if (!tipeStr.isEmpty()) {
            try {
                tipe = TipeAnggaran.valueOf(tipeStr);
            } catch (IllegalArgumentException e) {
                errors.add("Baris " + nomor + ": Tipe harus OPEX atau CAPEX, bukan '" + text(row, 2, fmt) + "'");
            }
        }
        Boolean aktif = null;
        String status = text(row, 4, fmt).toLowerCase(Locale.ROOT);
        switch (status) {
            case "" -> { }
            case "aktif", "ya", "true", "1" -> aktif = true;
            case "nonaktif", "non aktif", "tidak aktif", "tidak", "false", "0" -> aktif = false;
            default -> errors.add("Baris " + nomor + ": Status harus Aktif atau Nonaktif, bukan '"
                    + text(row, 4, fmt) + "'");
        }
        String kategori = text(row, 3, fmt);
        MataAnggaranDto.Request req = new MataAnggaranDto.Request(text(row, 0, fmt), text(row, 1, fmt), tipe,
                kategori.isEmpty() ? null : kategori, aktif);

        // Aturan field sama dengan form Master Data (anotasi pada MataAnggaranDto.Request).
        // Satu pesan per kolom; "wajib diisi" didahulukan dari pesan format.
        Map<String, String> perKolom = new TreeMap<>(Comparator.comparingInt(k -> List.of(HEADER).indexOf(k)));
        for (ConstraintViolation<MataAnggaranDto.Request> v : validator.validate(req)) {
            if (v.getPropertyPath().toString().equals("tipe") && !tipeStr.isEmpty()) {
                continue; // tipe tidak dikenal sudah dilaporkan di atas
            }
            String p = pesan(v);
            perKolom.merge(label(v), p, (lama, baru) -> baru.equals("wajib diisi") ? baru : lama);
        }
        perKolom.forEach((kolom, p) -> errors.add("Baris " + nomor + ": " + kolom + " " + p));

        return errors.size() > awal ? Optional.empty() : Optional.of(req);
    }

    private static String label(ConstraintViolation<?> v) {
        return KOLOM.getOrDefault(v.getPropertyPath().toString(), v.getPropertyPath().toString());
    }

    private static String pesan(ConstraintViolation<?> v) {
        String anotasi = v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        return switch (anotasi) {
            case "NotBlank", "NotNull" -> "wajib diisi";
            case "Pattern" -> "harus berupa angka 4-30 digit";
            case "Size" -> "maksimal " + v.getConstraintDescriptor().getAttributes().get("max") + " karakter";
            default -> v.getMessage();
        };
    }

    private static void row(Sheet sheet, int r, String... values) {
        Row row = sheet.createRow(r);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
