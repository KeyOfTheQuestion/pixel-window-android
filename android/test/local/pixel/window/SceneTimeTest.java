package local.pixel.window;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public final class SceneTimeTest {
    public static void main(String[] args) {
        SceneTime.SolarDay summer = SceneTime.solarDay(LocalDate.of(2026, 6, 21));
        SceneTime.SolarDay winter = SceneTime.solarDay(LocalDate.of(2026, 12, 22));
        SceneTime.SolarDay november = SceneTime.solarDay(LocalDate.of(2026, 11, 18));
        near(summer.sunrise, 4 * 60 + 45, 3);
        near(summer.sunset, 19 * 60 + 46, 3);
        near(winter.sunrise, 7 * 60 + 32, 3);
        near(winter.sunset, 16 * 60 + 52, 3);
        near(november.sunrise, 7 * 60 + 2, 3);
        near(november.sunset, 16 * 60 + 56, 3);
        near(november.nightMix(november.sunrise), .5, .001);
        near(november.nightMix(november.sunset), .5, .001);
        check(november.nightMix(november.sunrise - 15) > november.nightMix(november.sunrise + 15));
        check(november.nightMix(november.sunset - 15) < november.nightMix(november.sunset + 15));
        check(november.nightMix(0) == 1 && november.nightMix(720) == 0 && november.nightMix(1439) == 1);
        check(summer.nightMix(18 * 60) == 0 && winter.nightMix(18 * 60) == 1);
        var beijing = SceneTime.beijing(Instant.parse("2026-11-17T23:02:00Z"));
        check(beijing.toLocalDate().equals(LocalDate.of(2026, 11, 18)));
        check(beijing.toLocalTime().equals(LocalTime.of(7, 2)));
        check(SceneTime.phase(1_999, 2_000, 24_000) == 23_999);
        check(SceneTime.phase(2_000, 2_000, 24_000) == 0);
        check(SceneTime.phase(26_001, 2_000, 24_000) == 1);
        System.out.println("Day/night boundaries and recurring activities passed");
    }

    private static void near(double actual, double expected, double tolerance) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError("Expected " + expected + " +/- " + tolerance + ", got " + actual);
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("Unexpected scene time");
        }
    }
}
