package vn.edu.fpt.myfschool.common.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExcelReader {

    public List<Map<String, String>> read(InputStream is) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return rows;
            }

            List<String> headers = new ArrayList<>();
            for (int cellNum = 0; cellNum < headerRow.getLastCellNum(); cellNum++) {
                Cell cell = headerRow.getCell(cellNum);
                headers.add(cell != null ? getCellValueAsString(cell).trim() : "");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowMap = new HashMap<>();
                boolean isEmpty = true;
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    if (header.isEmpty()) continue;

                    Cell cell = row.getCell(j);
                    String val = cell != null ? getCellValueAsString(cell).trim() : "";
                    if (!val.isEmpty()) {
                        isEmpty = false;
                    }
                    rowMap.put(header, val);
                }
                if (!isEmpty) {
                    rows.add(rowMap);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage(), e);
        }
        return rows;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numericValue = cell.getNumericCellValue();
                // If it is mathematically an integer, format as integer to avoid trailing decimals (like .0)
                if (numericValue == (long) numericValue) {
                    return String.format("%d", (long) numericValue);
                } else {
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
