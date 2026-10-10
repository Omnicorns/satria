package id.co.realisasi.excel;

import id.co.realisasi.domain.Divisi;
import id.co.realisasi.domain.MataAnggaran;
import id.co.realisasi.domain.TipeAnggaran;
import id.co.realisasi.repository.DivisiRepository;
import id.co.realisasi.repository.MataAnggaranRepository;
import id.co.realisasi.service.BusinessException;
import id.co.realisasi.service.ProgramKerjaService;
import id.co.realisasi.web.dto.ImportResult;
import id.co.realisasi.web.dto.ProgramKerjaDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Template, export dan import Excel untuk Monitoring RKA.
 * Anggaran diisi per divisi. Kunci baris import: kode divisi + tahun + no COA (upsert).
 */
@Service
@RequiredArgsConstructor
public class RkaExcelService {

    static final String[] HEADER_IMPORT = {
            "Kode Divisi", "Tahun", "No COA", "Nama Mata Anggaran", "Planning Budget", "Pagu", "Keterangan"
    };
    static final String[] HEADER_EXPORT = {
            "Perusahaan", "Kode Divisi", "Divisi", "Tahun", "Tipe", "No COA", "Nama Mata Anggaran",
            "Planning Budget", "Pagu", "Realisasi", "Sisa", "Capaian (%)", "Status", "Keterangan"
    };

    private final DivisiRepository divisiRepository;
    private final MataAnggaranRepository mataAnggaranRepository;
    private final ProgramKerjaService programKerjaService;

    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("RKA");
            header(wb, sheet, HEADER_IMPORT);
            Row contoh = sheet.createRow(1);
            contoh.createCell(0).setCellValue("INJ-TG");
            contoh.createCell(1).setCellValue(2027);
            contoh.createCell(2).setCellValue("6104050001");
            contoh.createCell(3).setCellValue("Beban Sewa Mesin dan Peralatan (opsional, info saja)");
            contoh.createCell(4).setCellValue(1732464000);
            contoh.createCell(6).setCellValue("Contoh baris, hapus sebelum import");

            Sheet div = wb.createSheet("Daftar Divisi");
            header(wb, div, new String[]{"Kode Divisi", "Nama Divisi", "Perusahaan"});
            int d = 1;
            for (Divisi x : divisiRepository.search(null)) {
                Row row = div.createRow(d++);
                row.createCell(0).setCellValue(x.getKode());
                row.createCell(1).setCellValue(x.getNama());
                row.createCell(2).setCellValue(x.getCompany().getNama());
            }
            autosize(div, 3);

            Sheet coa = wb.createSheet("Daftar COA");
            header(wb, coa, new String[]{"No COA", "Nama Mata Anggaran", "Tipe"});
            int r = 1;
            for (MataAnggaran m : mataAnggaranRepository.findAllByOrderByNoCoa()) {
                Row row = coa.createRow(r++);
                row.createCell(0).setCellValue(m.getNoCoa());
                row.createCell(1).setCellValue(m.getNama());
                row.createCell(2).setCellValue(m.getTipe().name());
            }
            autosize(sheet, HEADER_IMPORT.length);
            autosize(coa, 3);
            return write(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] export(Long companyId, Long divisiId, Integer tahun, TipeAnggaran tipe) {
        List<ProgramKerjaDto.Response> list = programKerjaService.list(companyId, divisiId, tahun, tipe);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Monitoring RKA");
            header(wb, sheet, HEADER_EXPORT);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            int r = 1;
            for (ProgramKerjaDto.Response p : list) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(p.companyNama());
                row.createCell(1).setCellValue(p.divisiKode());
                row.createCell(2).setCellValue(p.divisiNama());
                row.createCell(3).setCellValue(p.tahun());
                row.createCell(4).setCellValue(p.tipe().name());
                row.createCell(5).setCellValue(p.noCoa());
                row.createCell(6).setCellValue(p.namaMataAnggaran());
                money(row, 7, p.planningBudget(), money);
                money(row, 8, p.pagu(), money);
                money(row, 9, p.realisasi(), money);
                money(row, 10, p.sisa(), money);
                row.createCell(11).setCellValue(p.capaianPersen());
                row.createCell(12).setCellValue(p.status().name());
                row.createCell(13).setCellValue(p.keterangan() == null ? "" : p.keterangan());
            }
            autosize(sheet, HEADER_EXPORT.length);
            return write(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Import seluruh baris; bila ada satu baris tidak valid, tidak ada yang disimpan. */
    @Transactional
    public ImportResult importExcel(InputStream in) {
        List<ParsedRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(row, fmt)) {
                    continue;
                }
                parse(row, fmt, i + 1, errors).ifPresent(rows::add);
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException("File bukan Excel yang valid: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            return new ImportResult(rows.size() + errors.size(), 0, 0, errors);
        }
        int dibuat = 0;
        int diperbarui = 0;
        for (ParsedRow p : rows) {
            boolean baru = programKerjaService.upsert(p.divisi, p.mata, p.tahun, p.planning, p.pagu, p.keterangan);
            if (baru) {
                dibuat++;
            } else {
                diperbarui++;
            }
        }
        return new ImportResult(rows.size(), dibuat, diperbarui, List.of());
    }

    private Optional<ParsedRow> parse(Row row, DataFormatter fmt, int nomor, List<String> errors) {
        int awal = errors.size();
        String kode = text(row, 0, fmt);
        String tahunStr = text(row, 1, fmt);
        String coa = text(row, 2, fmt);

        Divisi divisi = null;
        if (kode.isEmpty()) {
            errors.add("Baris " + nomor + ": Kode Divisi wajib diisi");
        } else {
            divisi = divisiRepository.findByKode(kode).orElse(null);
            if (divisi == null) {
                errors.add("Baris " + nomor + ": Kode Divisi " + kode + " tidak ditemukan");
            }
        }
        Integer tahun = null;
        try {
            tahun = Integer.valueOf(tahunStr);
            if (tahun < 2000 || tahun > 2100) {
                errors.add("Baris " + nomor + ": Tahun " + tahunStr + " di luar rentang");
            }
        } catch (NumberFormatException e) {
            errors.add("Baris " + nomor + ": Tahun tidak valid '" + tahunStr + "'");
        }
        MataAnggaran mata = null;
        if (coa.isEmpty()) {
            errors.add("Baris " + nomor + ": No COA wajib diisi");
        } else {
            mata = mataAnggaranRepository.findByNoCoa(coa).orElse(null);
            if (mata == null) {
                errors.add("Baris " + nomor + ": No COA " + coa + " tidak terdaftar");
            }
        }
        BigDecimal planning = number(row, 4, fmt, nomor, "Planning Budget", true, errors);
        BigDecimal pagu = number(row, 5, fmt, nomor, "Pagu", false, errors);
        String ket = text(row, 6, fmt);

        if (errors.size() > awal) {
            return Optional.empty();
        }
        return Optional.of(new ParsedRow(divisi, mata, tahun, planning, pagu, ket.isEmpty() ? null : ket));
    }

    private static BigDecimal number(Row row, int col, DataFormatter fmt, int nomor, String nama, boolean wajib,
                                     List<String> errors) {
        Cell cell = row.getCell(col);
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            BigDecimal v = BigDecimal.valueOf(cell.getNumericCellValue());
            if (v.signum() < 0) {
                errors.add("Baris " + nomor + ": " + nama + " tidak boleh negatif");
            }
            return v;
        }
        String s = text(row, col, fmt).replace(".", "").replace(",", ".").replace("Rp", "").trim();
        if (s.isEmpty()) {
            if (wajib) {
                errors.add("Baris " + nomor + ": " + nama + " wajib diisi");
            }
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.signum() < 0) {
                errors.add("Baris " + nomor + ": " + nama + " tidak boleh negatif");
            }
            return v;
        } catch (NumberFormatException e) {
            errors.add("Baris " + nomor + ": " + nama + " bukan angka '" + s + "'");
            return null;
        }
    }

    private static String text(Row row, int col, DataFormatter fmt) {
        Cell cell = row.getCell(col);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }

    private static boolean isBlank(Row row, DataFormatter fmt) {
        for (int c = 0; c < HEADER_IMPORT.length; c++) {
            if (!text(row, c, fmt).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void header(Workbook wb, Sheet sheet, String[] cols) {
        CellStyle style = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        style.setFont(bold);
        Row h = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void money(Row row, int col, BigDecimal v, CellStyle style) {
        Cell c = row.createCell(col);
        if (v != null) {
            c.setCellValue(v.doubleValue());
            c.setCellStyle(style);
        }
    }

    private static void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.setColumnWidth(i, 22 * 256);
        }
    }

    private static byte[] write(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private record ParsedRow(Divisi divisi, MataAnggaran mata, Integer tahun, BigDecimal planning,
                             BigDecimal pagu, String keterangan) {
    }
}
