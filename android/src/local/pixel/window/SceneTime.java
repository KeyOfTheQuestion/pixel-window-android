package local.pixel.window;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class SceneTime {
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final double LATITUDE = Math.toRadians(39.9042);
    private static final double LONGITUDE = 116.4074;

    private SceneTime() {}

    static ZonedDateTime beijing(Instant instant) {
        return instant.atZone(BEIJING);
    }

    static SolarDay solarDay(LocalDate date) {
        // NOAA fractional-year approximation, using Beijing's current UTC+8 civil time.
        double year = 2 * Math.PI * (date.getDayOfYear() - 1) / date.lengthOfYear();
        double sin = Math.sin(year);
        double cos = Math.cos(year);
        double sin2 = Math.sin(2 * year);
        double cos2 = Math.cos(2 * year);
        double equation = 229.18 * (.000075 + .001868 * cos - .032077 * sin - .014615 * cos2 - .040849 * sin2);
        double declination = .006918 - .399912 * cos + .070257 * sin - .006758 * cos2 + .000907 * sin2
            - .002697 * Math.cos(3 * year) + .00148 * Math.sin(3 * year);
        double noon = 720 + 8 * 60 - 4 * LONGITUDE - equation;
        double horizon = halfDay(declination, 90.833);
        double civilTwilight = halfDay(declination, 96);
        return new SolarDay(noon - horizon, noon + horizon, civilTwilight - horizon);
    }

    private static double halfDay(double declination, double zenith) {
        double cosine = (Math.cos(Math.toRadians(zenith)) - Math.sin(LATITUDE) * Math.sin(declination))
            / (Math.cos(LATITUDE) * Math.cos(declination));
        return 4 * Math.toDegrees(Math.acos(cosine));
    }

    static final class SolarDay {
        final double sunrise;
        final double sunset;
        private final double twilight;

        SolarDay(double sunrise, double sunset, double twilight) {
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.twilight = twilight;
        }

        float nightMix(double minute) {
            double fromDay = Math.max(sunrise - minute, minute - sunset);
            double t = Math.max(0, Math.min(1, .5 + fromDay / (2 * twilight)));
            return (float) (t * t * (3 - 2 * t));
        }
    }

    static long phase(long elapsed, long offset, long period) {
        return Math.floorMod(elapsed - offset, period);
    }
}
