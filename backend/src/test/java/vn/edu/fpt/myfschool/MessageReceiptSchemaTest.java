package vn.edu.fpt.myfschool;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import org.junit.jupiter.api.Test;
import vn.edu.fpt.myfschool.entity.BaseEntity;
import vn.edu.fpt.myfschool.entity.MessageReceipt;

import static org.assertj.core.api.Assertions.assertThat;

class MessageReceiptSchemaTest {

    @Test
    void receipt_foreign_keys_match_unsigned_mysql_ids() throws NoSuchFieldException {
        assertUnsignedJoinColumn("message");
        assertUnsignedJoinColumn("user");
    }

    private void assertUnsignedJoinColumn(String fieldName) throws NoSuchFieldException {
        JoinColumn joinColumn = MessageReceipt.class
                .getDeclaredField(fieldName)
                .getAnnotation(JoinColumn.class);

        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.columnDefinition()).isEqualTo("BIGINT UNSIGNED");
    }
}
