import { CatMotion, HEIGHT, SILL, WIDTH, beijingShade, bikeAt, doorOpenness, phase, visitorAt } from './model.mjs';
import { drawBike, drawCat, drawVisitor } from './actors.mjs';
import { bookshopDoor } from './door.mjs';

const canvas = document.querySelector('canvas');
const context = canvas.getContext('2d', { alpha: false });
const status = document.querySelector('#status');
const dialog = document.querySelector('#options');
const options = [...dialog.querySelectorAll('[data-light]')];
const awake = document.querySelector('#awake');
const offlineStatus = document.querySelector('#offline-status');
const files = { day: 'day.jpg', night: 'night.jpg', readers: 'readers.png', cat: 'cat.png', sleep: 'sleep.png' };
const cat = new CatMotion();
let mode = 'auto';
let wakeLock;
let frameId;
let shade = 0;
let nextShadeCheck = 0;

document.querySelector('#settings').addEventListener('click', () => dialog.showModal());
for (const option of options) {
  option.addEventListener('click', () => {
    mode = option.dataset.light;
    nextShadeCheck = 0;
    for (const item of options) {
      item.setAttribute('aria-pressed', String(item === option));
    }
    dialog.close();
  });
}

document.querySelector('#awake-option').hidden = !navigator.wakeLock;
awake.addEventListener('change', refreshWakeLock);

async function refreshWakeLock() {
  if (!awake.checked) {
    await wakeLock?.release();
    wakeLock = undefined;
    return;
  }
  if (document.hidden || (wakeLock && !wakeLock.released)) {
    return;
  }
  try {
    const lock = await navigator.wakeLock.request('screen');
    if (awake.checked && !document.hidden) {
      wakeLock = lock;
    } else {
      await lock.release();
    }
  } catch {
    awake.checked = false;
    document.querySelector('#awake-option').title = '当前设备未能保持亮屏';
  }
}

function loadImage(name) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(name));
    img.src = './assets/' + name;
  });
}

function fill(x, y, w, h, color) {
  context.fillStyle = color;
  context.fillRect(x, y, w, h);
}

function windowFrame() {
  fill(0, SILL, WIDTH, HEIGHT - SILL, '#4e3028');
  fill(0, SILL + 1, WIDTH, 2, '#966144');
  fill(0, 193, WIDTH, 1, '#73503d');
  fill(0, 0, WIDTH, 4, '#2b1c20');
  fill(0, 0, 4, HEIGHT, '#2b1c20');
  fill(WIDTH - 4, 0, 4, HEIGHT, '#2b1c20');
  fill(4, 4, WIDTH - 8, 1, '#815844');
  fill(25, 176, 12, 12, '#af7258');
  fill(28, 179, 2, 8, '#d19a71');
  fill(31, 162, 2, 15, '#4e7360');
  fill(24, 168, 7, 3, '#8b9965');
  fill(32, 163, 5, 4, '#8b9965');
  fill(34, 166, 5, 3, '#4e7360');
  fill(46, 183, 21, 5, '#a35a54');
  fill(47, 180, 18, 4, '#678575');
  fill(49, 181, 14, 2, '#d5b599');
  if (shade > 0) {
    context.globalAlpha = shade * 0.32;
    fill(0, SILL, WIDTH, HEIGHT - SILL, '#161325');
    context.globalAlpha = 1;
  }
}

function air(elapsed) {
  for (let i = 0; i < 7; i++) {
    const drift = phase(elapsed, 735 * i, 4250 + i * 165);
    if (drift > 3350) {
      continue;
    }
    const t = drift / 3350;
    const x = 8 + i * 18 + 73 * t;
    const y = 32 + i * 13 + 45 * t + 6 * Math.sin(9 * t + i);
    context.globalAlpha = 0.9 - 0.55 * shade;
    fill(Math.round(x), Math.round(y), 2, 1, i % 2 ? '#cf743d' : '#efaa62');
  }
  context.globalAlpha = 1;
}

function resize() {
  const ratio = Math.min(devicePixelRatio || 1, 2.5, 1860 / canvas.clientWidth);
  canvas.width = Math.max(1, Math.round(canvas.clientWidth * ratio));
  canvas.height = Math.max(1, Math.round(canvas.clientHeight * ratio));
}

async function start() {
  const images = Object.fromEntries(await Promise.all(Object.entries(files).map(async ([key, file]) => {
    return [key, await loadImage(file)];
  })));
  const drawDoor = bookshopDoor(images.day, images.night);
  const started = performance.now();
  resize();
  window.addEventListener('resize', resize);
  status.hidden = true;
  document.body.classList.add('ready');

  function draw(now) {
    const elapsed = Math.max(0, now - started);
    if (now >= nextShadeCheck) {
      shade = mode === 'auto' ? beijingShade() : mode === 'day' ? 0 : 1;
      nextShadeCheck = now + 2000;
    }
    const scale = Math.min(canvas.width / WIDTH, canvas.height / HEIGHT);
    context.setTransform(1, 0, 0, 1, 0, 0);
    fill(0, 0, canvas.width, canvas.height, '#17121d');
    context.setTransform(scale, 0, 0, scale, (canvas.width - scale * WIDTH) / 2, (canvas.height - scale * HEIGHT) / 2);
    if (shade < 1) {
      context.drawImage(images.day, 0, 0, WIDTH, HEIGHT);
    }
    if (shade > 0) {
      context.globalAlpha = shade;
      context.drawImage(images.night, 0, 0, WIDTH, HEIGHT);
      context.globalAlpha = 1;
    }
    context.save();
    context.beginPath();
    context.rect(4, 5, WIDTH - 8, SILL - 5);
    context.clip();
    const visitors = [0, 1, 2].map(person => visitorAt(elapsed, person)).filter(Boolean);
    const opening = visitors.find(visitor => visitor.stage === 'enter' || visitor.stage === 'exit');
    if (opening) {
      const open = doorOpenness(opening.progress);
      if (open > 0 && shade < 1) {
        drawDoor(context, 0, open);
      }
      if (open > 0 && shade > 0) {
        context.globalAlpha = shade;
        drawDoor(context, 1, open);
        context.globalAlpha = 1;
      }
    }
    for (const visitor of visitors) {
      drawVisitor(context, images.readers, visitor, elapsed, shade);
    }
    const bike = bikeAt(elapsed);
    if (bike) {
      drawBike(context, bike, shade);
    }
    air(elapsed);
    context.restore();
    windowFrame();
    context.save();
    context.beginPath();
    context.rect(4, 5, WIDTH - 8, HEIGHT - 5);
    context.clip();
    cat.update(elapsed);
    drawCat(context, images, cat, shade);
    context.restore();
    frameId = requestAnimationFrame(draw);
  }

  document.addEventListener('visibilitychange', () => {
    cancelAnimationFrame(frameId);
    if (!document.hidden) {
      nextShadeCheck = 0;
      frameId = requestAnimationFrame(draw);
      refreshWakeLock();
    }
  });
  if (!document.hidden) {
    frameId = requestAnimationFrame(draw);
  }
}

start().catch(error => {
  status.textContent = '窗景素材未能加载，请联网后刷新。';
  console.error(error);
});

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./sw.js').then(() => navigator.serviceWorker.ready).then(() => {
    offlineStatus.hidden = false;
    offlineStatus.textContent = '素材已缓存，可离线打开。';
  }).catch(error => console.warn('Offline cache:', error));
}
