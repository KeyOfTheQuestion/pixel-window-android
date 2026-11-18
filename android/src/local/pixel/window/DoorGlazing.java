package local.pixel.window;

import java.util.Arrays;

final class DoorGlazing {
    private DoorGlazing() {}

    static int[] separate(int[] door, int width, int height, int[] columns, int[] rows, int floorColor) {
        Sample[] horizontal = samples(width, columns);
        Sample[] vertical = samples(height, rows);
        int[] room = new int[door.length];
        int glassBottom = rows[rows.length - 1];
        for (int y = 0; y < height; y++) {
            Sample row = vertical[y];
            for (int x = 0; x < width; x++) {
                Sample col = horizontal[x];
                int upper = blend(door[row.low * width + col.low], door[row.low * width + col.high], col.weight);
                int lower = blend(door[row.high * width + col.low], door[row.high * width + col.high], col.weight);
                int color = blend(upper, lower, row.weight);
                if (y < rows[0]) {
                    color = blend(0xff392b23, color, .42f + .58f * y / rows[0]);
                } else if (y >= glassBottom) {
                    float depth = (y - glassBottom) / (float) (height - glassBottom);
                    color = blend(color, floor(floorColor, x, y, width, depth),
                        Math.min(1, (y - glassBottom + 1) / 13f));
                }
                room[y * width + x] = color;
            }
        }
        for (int row = 0; row < rows.length; row += 2) {
            for (int column = 0; column < columns.length; column += 2) {
                for (int y = rows[row]; y < rows[row + 1]; y++) {
                    Arrays.fill(door, y * width + columns[column], y * width + columns[column + 1], 0);
                }
            }
        }
        return room;
    }

    private static Sample[] samples(int length, int[] edges) {
        Sample[] samples = new Sample[length];
        int first = edges[0];
        int last = edges[edges.length - 1] - 1;
        for (int position = 0; position < length; position++) {
            int reflected = Math.max(first, Math.min(last, position));
            if (position < first) {
                reflected = Math.min(2 * first - position, edges[1] - 1);
            } else if (position > last) {
                reflected = Math.max(2 * last - position, edges[edges.length - 2]);
            }
            Sample sample = new Sample(reflected, reflected, 0);
            for (int edge = 1; edge < edges.length - 1; edge += 2) {
                int lo = edges[edge] - 1;
                int hi = edges[edge + 1];
                if (position > lo && position < hi) {
                    sample = new Sample(lo, hi, (position - lo) / (float) (hi - lo));
                    break;
                }
            }
            samples[position] = sample;
        }
        return samples;
    }

    private static int floor(int color, int x, int y, int width, float depth) {
        float across = (x - width * .48f) / (.55f + .65f * depth);
        float board = across / 19;
        float seam = Math.abs(board - Math.round(board));
        float light = 1 - Math.min(1, Math.abs(across) / (width * .65f));
        int grain = Math.floorMod(x * 23 + y * 17 + (x * y) % 41, 7) - 3;
        int variation = Math.round(10 * light + 8 * depth) + grain - (seam < .035f ? 7 : 0);
        return rgb((color >>> 16 & 255) + variation, (color >>> 8 & 255) + variation, (color & 255) + variation);
    }

    private static int blend(int from, int to, float weight) {
        return rgb(mix(from >>> 16 & 255, to >>> 16 & 255, weight),
            mix(from >>> 8 & 255, to >>> 8 & 255, weight),
            mix(from & 255, to & 255, weight));
    }

    private static int mix(int from, int to, float weight) {
        return Math.round(from + (to - from) * weight);
    }

    private static int rgb(int r, int g, int b) {
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    private static final class Sample {
        final int low;
        final int high;
        final float weight;

        Sample(int low, int high, float weight) {
            this.low = low;
            this.high = high;
            this.weight = weight;
        }
    }
}
