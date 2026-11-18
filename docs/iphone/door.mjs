import { DOOR, WIDTH, HEIGHT, doorRotation, mix, projectDoor } from './model.mjs';

const pixels = {
  day: { columns: [959, 982, 986, 1008, 1013, 1036], rows: [419, 455, 458, 494, 501, 536], floor: [87, 58, 40] },
  night: { columns: [958, 983, 988, 1010, 1015, 1040], rows: [419, 457, 461, 496, 502, 537], floor: [96, 57, 35] },
};

export function buffer(width, height) {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  return canvas.getContext('2d');
}

function axis(length, edges) {
  return Array.from({ length }, (_, position) => {
    const first = edges[0];
    const last = edges[edges.length - 1] - 1;
    if (position < first) {
      const i = Math.min(2 * first - position, edges[1] - 1);
      return [i, i, 0];
    }
    if (position > last) {
      const i = Math.max(2 * last - position, edges[edges.length - 2]);
      return [i, i, 0];
    }
    for (let i = 1; i < edges.length - 1; i += 2) {
      const lo = edges[i] - 1;
      const hi = edges[i + 1];
      if (position > lo && position < hi) {
        return [lo, hi, (position - lo) / (hi - lo)];
      }
    }
    return [position, position, 0];
  });
}

function artwork(image, mode) {
  const sx = image.width / WIDTH;
  const sy = image.height / HEIGHT;
  const left = Math.round(DOOR.x * sx);
  const top = Math.round(DOOR.y * sy);
  const width = Math.round((DOOR.x + DOOR.width) * sx) - left;
  const height = Math.round((DOOR.y + DOOR.height) * sy) - top;
  const panel = buffer(width, height);
  const room = buffer(width, height);
  panel.drawImage(image, left, top, width, height, 0, 0, width, height);
  const frame = panel.getImageData(0, 0, width, height);
  const interior = room.createImageData(width, height);
  const style = pixels[mode];
  const columns = style.columns.map(x => Math.round(x * image.width / 1860) - left);
  const rows = style.rows.map(y => Math.round(y * image.height / 846) - top);
  const xs = axis(width, columns);
  const ys = axis(height, rows);
  const bottom = rows[rows.length - 1];

  for (let y = 0; y < height; y++) {
    const [y0, y1, ty] = ys[y];
    for (let x = 0; x < width; x++) {
      const [x0, x1, tx] = xs[x];
      const index = (y * width + x) * 4;
      const depth = Math.max(0, (y - bottom) / (height - bottom));
      const across = (x - width * 0.48) / (0.55 + 0.65 * depth);
      const board = across / 19;
      const grain = (x * 23 + y * 17 + x * y % 41) % 7 - 3;
      const floorShade = 10 * (1 - Math.min(1, Math.abs(across) / (width * 0.65))) + 8 * depth
        + grain - (Math.abs(board - Math.round(board)) < 0.035 ? 7 : 0);
      for (let c = 0; c < 3; c++) {
        const sample = (ix, iy) => frame.data[(iy * width + ix) * 4 + c];
        let color = mix(mix(sample(x0, y0), sample(x1, y0), tx), mix(sample(x0, y1), sample(x1, y1), tx), ty);
        if (y < rows[0]) {
          color = mix([57, 43, 35][c], color, 0.42 + 0.58 * y / rows[0]);
        } else if (y >= bottom) {
          color = mix(color, style.floor[c] + floorShade, Math.min(1, (y - bottom + 1) / 13));
        }
        interior.data[index + c] = color;
      }
      interior.data[index + 3] = 255;
    }
  }

  for (let row = 0; row < rows.length; row += 2) {
    for (let col = 0; col < columns.length; col += 2) {
      panel.clearRect(columns[col], rows[row], columns[col + 1] - columns[col], rows[row + 1] - rows[row]);
    }
  }
  room.putImageData(interior, 0, 0);
  return { panel: panel.canvas, room: room.canvas, warp: buffer(width, height) };
}

export function bookshopDoor(day, night) {
  const art = [artwork(day, 'day'), artwork(night, 'night')];
  function draw(context, index, openness) {
    const { panel, room, warp } = art[index];
    const { width, height } = panel;
    const angle = doorRotation(openness);
    const farX = DOOR.width * Math.cos(angle);
    const depth = DOOR.width * Math.sin(angle);
    const count = Math.ceil(projectDoor(openness, 1).x / DOOR.width * width);
    warp.clearRect(0, 0, width, height);
    warp.drawImage(room, 0, 0);
    // Every destination column samples the same rigid plane through the fixed hinge.
    for (let column = 0; column < count; column++) {
      const x = (column + 0.5) / width * DOOR.width;
      const fraction = 72 * x / (72 * farX - (x - DOOR.width / 2) * depth);
      const source = Math.min(width - 1, Math.floor(fraction * width));
      const scale = 72 / (72 + fraction * depth);
      warp.drawImage(panel, source, 0, 1, height, column, (1 - scale) * height / 2, 1, height * scale);
    }
    context.drawImage(warp.canvas, DOOR.x, DOOR.y, DOOR.width, DOOR.height);
  }
  return draw;
}
