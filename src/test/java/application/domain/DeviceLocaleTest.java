package application.domain;

import application.domain.exceptions.UnknownDeviceException;
import application.domain.valueobjects.SurveillanceDevice;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** Locale is process-wide, so these cases must not overlap other tests. */
@Isolated
class DeviceLocaleTest {
    @ParameterizedTest
    @ValueSource(strings = {"tr-TR", "az-AZ", "en-US", "es-CO"})
    void identifiersDoNotDependOnSystemLanguage(String languageTag) {
        Locale original = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.forLanguageTag(languageTag));
            assertEquals(SurveillanceDevice.STAR_LABS, SurveillanceDevice.from("star-labs"));
            assertEquals(SurveillanceDevice.CCPD, SurveillanceDevice.from("ccpd"));
            for (String id : new String[]{"S.T.A.R. Satellite", "star_satellite", "star-satellite",
                    " STAR Satellite ", "STAR_SATELLITE"}) {
                assertEquals(SurveillanceDevice.STAR_SATELLITE, SurveillanceDevice.from(id), id);
            }
            assertThrows(UnknownDeviceException.class, () -> SurveillanceDevice.from("invalid"));
            assertThrows(UnknownDeviceException.class, () -> SurveillanceDevice.from(" "));
            assertThrows(UnknownDeviceException.class, () -> SurveillanceDevice.from(null));
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }
}
