package vn.edu.fpt.myfschool.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.fpt.myfschool.common.enums.ConductSource;

import java.util.Locale;

@Converter
public class ConductSourceConverter implements AttributeConverter<ConductSource, String> {

    private static final String LEGACY_HOMEROOM = "HOMEROOM";

    @Override
    public String convertToDatabaseColumn(ConductSource attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ConductSource convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_HOMEROOM.equals(normalized)) {
            return ConductSource.ADMIN;
        }
        return ConductSource.valueOf(normalized);
    }
}
