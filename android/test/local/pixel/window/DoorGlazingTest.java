package local.pixel.window;

import java.util.Arrays;

public final class DoorGlazingTest {
    public static void main(String[] args) {
        int width = 12;
        int height = 15;
        int[] columns = {1, 4, 5, 7, 8, 11};
        int[] rows = {1, 3, 4, 6, 7, 9};
        int wood = 0xff493a21;
        int[] door = new int[width * height];
        Arrays.fill(door, wood);
        for (int row = 0; row < rows.length; row += 2) {
            for (int column = 0; column < columns.length; column += 2) {
                for (int y = rows[row]; y < rows[row + 1]; y++) {
                    for (int x = columns[column]; x < columns[column + 1]; x++) {
                        door[y * width + x] = 0xffa64f20 + x + y * 256;
                    }
                }
            }
        }

        int[] original = door.clone();
        int[] room = DoorGlazing.separate(door, width, height, columns, rows, 0xff573623);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = y * width + x;
                if (original[pixel] == wood) {
                    check(door[pixel] == wood, "retain the wooden door surface and mullions");
                } else {
                    check(door[pixel] == 0, "no room detail remains on the moving glass");
                    check(room[pixel] == original[pixel], "retain the exact scene pixel behind each pane");
                }
                check((room[pixel] >>> 24) == 255, "the fixed room must cover the entire aperture");
            }
        }
        check(room[2 * width + 4] != wood, "fill the vertical mullion gap with the room behind it");
        check(room[3 * width + 6] != wood, "fill the horizontal mullion gap with the room behind it");
        System.out.println("Fixed room pixels and transparent door panes passed");
    }

    private static void check(boolean condition, String detail) {
        if (!condition) {
            throw new AssertionError(detail);
        }
    }
}
