package dev.charles.multigolem.internal.modstatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModStatusMessagesTest {
    @Test
    void defaultsProvideLabelsAndHelpText() {
        ModStatusMessages messages = ModStatusMessages.defaults();

        assertEquals("Matched", messages.labelFor(VersionStatus.MATCHED));
        assertEquals("Different versions", messages.labelFor(VersionStatus.DIFFERENT));
        assertEquals("Server version has not been received yet.", messages.helpFor(VersionStatus.UNKNOWN));
    }

    @Test
    void builderFallsBackForOmittedLabelsAndHelpText() {
        ModStatusMessages messages = ModStatusMessages.builder()
            .label(VersionStatus.MATCHED, "Same")
            .build();

        assertEquals("Same", messages.labelFor(VersionStatus.MATCHED));
        assertEquals("Different versions", messages.labelFor(VersionStatus.DIFFERENT));
        assertEquals("", messages.helpFor(VersionStatus.DIFFERENT));
    }

    @Test
    void builderRejectsNullAndBlankText() {
        assertThrows(NullPointerException.class, () -> ModStatusMessages.builder().label(null, "Label"));
        assertThrows(IllegalArgumentException.class, () -> ModStatusMessages.builder().label(VersionStatus.MATCHED, " "));
        assertThrows(NullPointerException.class, () -> ModStatusMessages.builder().help(null, "Help"));
        assertThrows(IllegalArgumentException.class, () -> ModStatusMessages.builder().help(VersionStatus.MATCHED, " "));
    }
}
