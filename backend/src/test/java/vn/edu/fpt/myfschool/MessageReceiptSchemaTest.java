package vn.edu.fpt.myfschool;

import jakarta.persistence.JoinColumn;
import org.junit.jupiter.api.Test;
import vn.edu.fpt.myfschool.controller.entity.MessageReceipt;

import static org.assertj.core.api.Assertions.assertThat;

class MessageReceiptSchemaTest {

    @Test
    void receipt_foreign_keys_use_base_entity_id_type() throws NoSuchFieldException {
        assertDefaultJoinColumn("message");
        assertDefaultJoinColumn("user");
    }

    private void assertDefaultJoinColumn(String fieldName) throws NoSuchFieldException {
        JoinColumn joinColumn = MessageReceipt.class
                .getDeclaredField(fieldName)
                .getAnnotation(JoinColumn.class);

        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.columnDefinition()).isEmpty();
    }
}
