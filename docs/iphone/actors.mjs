import { SILL, crossing, ease, mix, pawAt, restTime } from './model.mjs';

function box(ctx, x, y, w, h, color) {
  ctx.fillStyle = color;
  ctx.fillRect(x, y, w, h);
}

function oval(ctx, x, y, rx, ry, color) {
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2);
  ctx.fill();
}

function line(ctx, x, y, x2, y2, width, color) {
  ctx.beginPath();
  ctx.moveTo(x, y);
  ctx.lineTo(x2, y2);
  ctx.lineWidth = width;
  ctx.strokeStyle = color;
  ctx.stroke();
}

function tint(ctx, shade) {
  if (shade > 0 && 'filter' in ctx) {
    ctx.filter = 'brightness(' + (1 - 0.18 * shade) + ')';
  }
}

export function drawBike(ctx, ride, shade) {
  const ink = '#342a28';
  const metal = '#c7a574';
  const skin = '#e7a879';
  ctx.save();
  ctx.translate(ride.x, 182);
  ctx.scale(ride.right ? 1 : -1, 1);
  tint(ctx, shade);
  oval(ctx, 0, 1, 21, 1, '#332b3d55');
  const rotation = ride.elapsed / 97;
  for (const x of [-13, 13]) {
    oval(ctx, x, -6, 6, 6, ink);
    oval(ctx, x, -6, 4.5, 4.5, '#bfb095');
    oval(ctx, x, -6, 3.5, 3.5, '#5e5858');
    const dx = 4 * Math.cos(rotation);
    const dy = 4 * Math.sin(rotation);
    line(ctx, x - dx, -6 - dy, x + dx, -6 + dy, 1, '#c6b59c');
    line(ctx, x - dy, -6 + dx, x + dy, -6 - dx, 1, '#b6aa95');
    box(ctx, x - 1, -7, 2, 2, '#ddbe85');
  }
  line(ctx, -13, -6, -4, -14, 2, metal);
  line(ctx, -13, -6, -2, -6, 2, metal);
  line(ctx, -2, -6, 6, -15, 2, metal);
  line(ctx, -4, -14, 6, -15, 2, '#e0ba7b');
  line(ctx, -4, -14, -2, -6, 2, '#e0ba7b');
  line(ctx, 6, -15, 13, -6, 2, metal);
  line(ctx, 6, -15, 4, -20, 2, ink);
  box(ctx, 3, -21, 6, 2, ink);
  box(ctx, -8, -16, 7, 2, ink);
  box(ctx, 16, -11, 3, 2, '#d9a469');
  for (let side = 0; side < 2; side++) {
    const phase = ride.elapsed / 115 + side * Math.PI;
    const x = -2 + 4 * Math.cos(phase);
    const y = -7 + 4 * Math.sin(phase);
    const knee = (x + 3) * 0.65;
    const pants = side === 0 ? '#40444d' : '#55555a';
    line(ctx, -5, -17, knee, -12, 4, pants);
    line(ctx, knee, -12, x, y, 3, pants);
    box(ctx, x - 1, y, 5, 2, ink);
  }
  const bounce = Math.floor(ride.elapsed / 170) % 4 === 1 ? 1 : 0;
  box(ctx, -9, -23 + bounce, 8, 6, '#3f6660');
  box(ctx, -7, -25 + bounce, 8, 6, '#3f6660');
  box(ctx, -6, -24 + bounce, 2, 7, '#64877b');
  line(ctx, -2, -23 + bounce, 1, -20, 3, '#64877b');
  line(ctx, 1, -20, 7, -20, 2, skin);
  box(ctx, -2, -29 + bounce, 5, 5, skin);
  box(ctx, 2, -27 + bounce, 3, 2, skin);
  box(ctx, -3, -30 + bounce, 6, 2, '#603a35');
  box(ctx, -1, -31 + bounce, 4, 1, '#966152');
  box(ctx, 3, -29 + bounce, 2, 1, '#603a35');
  box(ctx, 2, -27 + bounce, 1, 1, ink);
  box(ctx, -5, -24 + bounce, 6, 2, '#c96d51');
  const scarf = Math.floor(ride.elapsed / 160) % 2;
  box(ctx, -12, -24 + scarf, 6, 2, '#c96d51');
  box(ctx, -15, -23 + scarf, 4, 1, '#e69e68');
  ctx.restore();
}

const bookX = [285.6, 346.6, 266.4];
const glassLeft = [275.4, 334.4, 259.7];
const glassRight = [295.3, 358.9, 273.1];
const tops = [94.1, 104.9, 118];
const bottoms = [103.8, 116.6, 130.4];
const walkFrames = [0, 1, 2, 1];

function pose(ctx, atlas, person, frame, x, floor, height, right, opacity) {
  if (opacity <= 0) {
    return;
  }
  ctx.save();
  ctx.globalAlpha *= opacity;
  ctx.translate(x, floor);
  ctx.scale(right ? -1 : 1, 1);
  const w = height * 209 / 346;
  ctx.drawImage(atlas, frame * 181 + 35, person * 181 + 2.5, 104.5, 173, -w / 2, -height, w, height);
  ctx.restore();
}

export function drawVisitor(ctx, atlas, route, elapsed, shade) {
  const { person, stage, progress } = route;
  ctx.save();
  if (stage === 'read') {
    ctx.beginPath();
    for (let i = 0; i < tops.length; i++) {
      ctx.rect(glassLeft[person], tops[i], glassRight[person] - glassLeft[person], bottoms[i] - tops[i]);
    }
    ctx.clip();
    const x = bookX[person];
    ctx.translate(x, 120);
    ctx.rotate(Math.sin(elapsed / 1450 + person) * 0.021);
    ctx.translate(-x, -120);
    pose(ctx, atlas, person, 3, x, 138.2, 32, false, 0.88 * ease(progress / 0.1) * ease((1 - progress) / 0.11));
  } else {
    tint(ctx, shade);
    const path = 159 + person * 0.85;
    let floor = path;
    let height = 34;
    let opacity = 1;
    let distance = route.distance;
    let standing = false;
    if (stage === 'enter' || stage === 'exit') {
      const depth = crossing(progress);
      const inside = stage === 'enter' ? depth : 1 - depth;
      floor = mix(path, 150, inside);
      height -= 4 * inside;
      opacity = ease((1 - inside) / 0.7);
      distance = 15.6 * depth;
      standing = inside === 0;
    }
    if (!standing) {
      floor -= 0.46 * Math.abs(Math.sin(distance * Math.PI / 10.4));
    }
    ctx.save();
    ctx.globalAlpha *= 0.36 * opacity;
    oval(ctx, route.x, floor + 0.2, 7, 1.05, '#2c2226');
    ctx.restore();
    const frame = standing ? 1 : walkFrames[Math.floor(distance / 5.2) % walkFrames.length];
    pose(ctx, atlas, person, frame, route.x, floor, height, route.right, opacity);
  }
  ctx.restore();
}

const rootsX = [101, 207, 127, 246];
const rootsY = [160, 153, 154, 152];
const toes = [99, 216, 120, 247];
const fur = ['#b67757', '#b67757', '#df9960', '#e0a06a'];
const artScale = 25 / 314;

function catLeg(ctx, i, bodyY, distance, strength) {
  const far = i < 2;
  const hind = i % 2 === 1;
  const paw = pawAt(distance, strength, i);
  const root = rootsX[i];
  const rootY = rootsY[i] + bodyY;
  const foot = toes[i] + paw.x / artScale;
  const lift = paw.lift / artScale;
  const ground = (far ? 230 : 234) - lift;
  const joint = 0.4 * root + 0.6 * foot + (hind ? 13 + 0.36 * lift : 2 + 0.16 * lift);
  const jointY = 0.49 * rootY + 0.51 * ground - 0.16 * lift;
  const thigh = hind ? 16 : 13;
  const shape = new Path2D();
  shape.moveTo(root - thigh, rootY - 10);
  shape.quadraticCurveTo(root, rootY - 20, root + thigh, rootY - 6);
  shape.quadraticCurveTo(joint + 14, jointY, foot + 11, ground - 11);
  shape.quadraticCurveTo(foot + 13, ground, foot + 2, ground);
  shape.quadraticCurveTo(foot - 16, ground + 1, foot - 14, ground - 7);
  shape.quadraticCurveTo(foot - 13, ground - 11, foot - 8, ground - 13);
  shape.quadraticCurveTo(joint - 12, jointY + 2, root - thigh, rootY - 10);
  shape.closePath();
  ctx.fillStyle = fur[i];
  ctx.fill(shape);
  ctx.save();
  ctx.clip(shape);
  line(ctx, joint - 13, jointY - 4, joint + 13, jointY + 2, far ? 5 : 7, far ? '#96583d' : '#b67043');
  oval(ctx, foot - 1.5, ground - 6.5, 14.5, 7.5, far ? '#c89474' : '#f4c9a0');
  ctx.restore();
  ctx.lineWidth = 3;
  ctx.lineJoin = 'round';
  ctx.strokeStyle = far ? '#5e382d' : '#79482c';
  ctx.stroke(shape);
}

function standingCat(ctx, image, x, right, distance, strength, crouch, opacity) {
  if (opacity <= 0) {
    return;
  }
  const rise = strength * 0.2 * Math.cos(4 * Math.PI * distance / 12.2);
  const bodyY = 6 + crouch - rise / artScale;
  ctx.save();
  ctx.globalAlpha *= opacity;
  ctx.translate(x, SILL + 1);
  ctx.scale(right ? -artScale : artScale, artScale);
  ctx.translate(-157, -234);
  for (let i = 0; i < rootsX.length; i++) {
    catLeg(ctx, i, bodyY, distance, strength);
  }
  ctx.translate(0, bodyY);
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.lineTo(314, 0);
  ctx.lineTo(314, 170);
  ctx.lineTo(263, 170);
  ctx.quadraticCurveTo(234, 185, 216, 160);
  ctx.quadraticCurveTo(181, 174, 148, 170);
  ctx.quadraticCurveTo(126, 185, 105, 175);
  ctx.lineTo(89, 166);
  ctx.lineTo(0, 166);
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(image, 0, 0, 314, 234);
  ctx.restore();
}

export function drawCat(ctx, images, motion, shade) {
  const { stage, x, age } = motion;
  if (stage === 'offscreen') {
    return;
  }
  ctx.save();
  tint(ctx, shade);
  oval(ctx, x, SILL + 1, 12.5, 1, '#241a1e4e');
  if (stage === 'rest') {
    const curl = ease(Math.min(age / 300, (restTime - age) / 420));
    standingCat(ctx, images.cat, x, false, 0, 0, 0, 1 - curl);
    if (curl > 0) {
      ctx.globalAlpha *= curl;
      const height = 25 * 252 / 486 * (1 + 0.01 * Math.sin(age / 650));
      ctx.drawImage(images.sleep, x - 12.5, SILL + 1 - height, 25, height);
    }
  } else {
    const crouch = stage === 'alert' ? 16 * ease(age / 144)
      : stage === 'wait' ? 16 : stage === 'retreat' ? 16 * (1 - ease(age / 180)) : 0;
    const strength = stage === 'alert' || stage === 'wait' ? 0 : motion.strength;
    standingCat(ctx, images.cat, x, motion.right, motion.distance, strength, crouch, 1);
  }
  ctx.restore();
}
