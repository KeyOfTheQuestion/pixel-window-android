export const WIDTH = 440;
export const HEIGHT = 200;
export const SILL = 187;
export const DOOR = { x: 224.1, y: 95.6, width: 23.8, height: 52.9 };
const doorAngle = 76 * Math.PI / 180;
export const mix = (a, b, t) => a + (b - a) * t;
export const phase = (time, offset, duration) => ((time - offset) % duration + duration) % duration;

export function ease(value) {
  const t = Math.max(0, Math.min(1, value));
  return t * t * (3 - 2 * t);
}

export function solarDay(dayOfYear, daysInYear) {
  const year = 2 * Math.PI * (dayOfYear - 1) / daysInYear;
  const sin = Math.sin(year);
  const cos = Math.cos(year);
  const sin2 = Math.sin(2 * year);
  const cos2 = Math.cos(2 * year);
  const equation = 229.18 * (0.000075 + 0.001868 * cos - 0.032077 * sin - 0.014615 * cos2 - 0.040849 * sin2);
  const declination = 0.006918 - 0.399912 * cos + 0.070257 * sin - 0.006758 * cos2 + 0.000907 * sin2
    - 0.002697 * Math.cos(3 * year) + 0.00148 * Math.sin(3 * year);
  const noon = 720 + 480 - 4 * 116.4074 - equation;
  const latitude = 39.9042 * Math.PI / 180;
  const halfDay = zenith => 720 / Math.PI * Math.acos(
    (Math.cos(zenith * Math.PI / 180) - Math.sin(latitude) * Math.sin(declination))
      / (Math.cos(latitude) * Math.cos(declination))
  );
  const horizon = halfDay(90.833);
  return { sunrise: noon - horizon, sunset: noon + horizon, twilight: halfDay(96) - horizon };
}

export function beijingShade(now = Date.now()) {
  const day = new Date(now + 8 * 60 * 60 * 1000);
  const year = day.getUTCFullYear();
  const jan = Date.UTC(year, 0, 1);
  const ordinal = Math.floor((day.getTime() - jan) / 86400000) + 1;
  const sun = solarDay(ordinal, (Date.UTC(year + 1, 0, 1) - jan) / 86400000);
  const minutes = day.getUTCHours() * 60 + day.getUTCMinutes() + day.getUTCSeconds() / 60;
  return ease(0.5 + Math.max(sun.sunrise - minutes, minutes - sun.sunset) / (2 * sun.twilight));
}

export const doorOpenness = progress => ease(Math.min(progress / 0.30, (1 - progress) / 0.30));
export const crossing = progress => ease((progress - 0.30) / 0.40);
export const doorRotation = openness => doorAngle * openness;

export function projectDoor(openness, fraction) {
  const angle = doorRotation(openness);
  const depth = fraction * DOOR.width * Math.sin(angle);
  const scale = 72 / (72 + depth);
  return { x: DOOR.width / 2 + (fraction * DOOR.width * Math.cos(angle) - DOOR.width / 2) * scale, scale };
}

const passage = DOOR.x + (DOOR.width + projectDoor(1, 1).x) / 2;
const arrivals = [11200, 15500, 29600];

export function visitorAt(elapsed, person) {
  const right = (Math.floor(elapsed / 56000) + person) % 2 === 0;
  const start = right ? -14 : 454;
  const end = right ? 454 : -14;
  const inTime = Math.round(Math.abs(passage - start) / 0.024);
  const outTime = Math.round(Math.abs(passage - end) / 0.024);
  const readTime = 5700 + person * 600;
  let time = phase(elapsed, 0, 56000) - arrivals[person] + inTime;
  function state(stage, duration, from = passage, to = passage) {
    const progress = time / duration;
    const x = mix(from, to, progress);
    return { person, stage, right, x, progress, distance: Math.abs(x - from) };
  }
  if (time < 0) {
    return null;
  }
  if (time < inTime) {
    return state('approach', inTime, start, passage);
  }
  if ((time -= inTime) < 3600) {
    return state('enter', 3600);
  }
  if ((time -= 3600) < readTime) {
    return state('read', readTime);
  }
  if ((time -= readTime) < 3500) {
    return state('exit', 3500);
  }
  if ((time -= 3500) < outTime) {
    return state('depart', outTime, passage, end);
  }
  return null;
}

const firstRide = 2400;
const ridePeriod = 28000;
const rideLength = 6600;
const speed = 498 / rideLength;
const passAt = time => Math.floor((time - firstRide) / ridePeriod);

export function bikeAt(time) {
  const elapsed = phase(time, firstRide, ridePeriod);
  if (time < firstRide || elapsed >= rideLength) {
    return null;
  }
  const right = passAt(time) % 2 === 0;
  return { elapsed, right, x: right ? -29 + elapsed * speed : 469 - elapsed * speed };
}

const step = 16;
const firstCat = 5900;
const catPeriod = 47000;
const shelter = 451;
const restingSpot = 358;
export const restTime = 4500;

export class CatMotion {
  stage = 'offscreen';
  cycle = -1;
  time = 0;
  since = 0;
  x = shelter;
  distance = 0;
  lastBike = -1;
  afterWait = 'enter';
  bikeFromLeft = false;

  get age() {
    return this.time - this.since;
  }

  get right() {
    return this.stage === 'leave' || this.stage === 'retreat'
      || ((this.stage === 'alert' || this.stage === 'wait') && this.afterWait === 'leave');
  }

  get strength() {
    const remaining = this.stage === 'enter' ? this.x - restingSpot : shelter - this.x;
    return ease(Math.min(this.distance, remaining) / 2.3);
  }

  update(elapsed) {
    if (elapsed < firstCat) {
      return;
    }
    const cycle = Math.floor((elapsed - firstCat) / catPeriod);
    if (this.cycle !== cycle) {
      this.cycle = cycle;
      this.time = firstCat + catPeriod * cycle;
      this.x = shelter;
      this.lastBike = -1;
      this.change('enter');
    }
    while (this.time + step <= elapsed && this.stage !== 'offscreen') {
      this.time += step;
      this.advance();
    }
  }

  change(stage) {
    this.stage = stage;
    this.since = this.time;
    this.distance = 0;
  }

  move(goal, pace) {
    const distance = Math.min(Math.abs(goal - this.x), step * pace);
    this.x = distance === Math.abs(goal - this.x) ? goal : this.x + Math.sign(goal - this.x) * distance;
    this.distance += distance;
  }

  advance() {
    const ride = bikeAt(this.time);
    const pass = passAt(this.time);
    if (ride && pass !== this.lastBike && this.x < 438
        && (this.stage === 'enter' || this.stage === 'rest' || this.stage === 'leave')) {
      const ahead = ride.right ? this.x - ride.x : ride.x - this.x;
      const notice = ride.right ? 44 + (shelter - this.x) * speed / 0.040 : 57;
      if (ahead >= 0 && ahead <= notice) {
        this.bikeFromLeft = ride.right;
        this.lastBike = pass;
        this.afterWait = this.stage === 'leave' ? 'leave' : 'enter';
        this.change('alert');
        return;
      }
    }
    switch (this.stage) {
      case 'enter':
        this.move(restingSpot, 0.016);
        if (this.x === restingSpot) {
          this.change('rest');
        }
        break;
      case 'rest':
        if (this.age >= restTime) {
          this.change('leave');
        }
        break;
      case 'leave':
        this.move(shelter, 0.016);
        if (this.x === shelter) {
          this.change('offscreen');
        }
        break;
      case 'alert':
        if (this.age >= 144) {
          this.change(this.bikeFromLeft ? 'retreat' : 'wait');
        }
        break;
      case 'retreat':
        this.move(shelter, 0.040);
        if (this.x === shelter) {
          this.change('wait');
        }
        break;
      case 'wait':
        if (this.age >= 192 && (pass !== this.lastBike || !ride || (!this.bikeFromLeft && ride.x < this.x - 45))) {
          this.change(this.afterWait === 'leave' && this.x === shelter ? 'offscreen' : this.afterWait);
        }
        break;
    }
  }
}

const pawPhases = [0.10, 0.29, 0.60, 0.79];

export function pawAt(distance, strength, index) {
  const cycle = distance / 12.2 + pawPhases[index];
  const stride = cycle - Math.floor(cycle);
  const swing = Math.max(0, (stride - 0.64) / 0.36);
  return {
    x: strength * 12.2 * (stride - 0.32 - ease(swing)),
    lift: strength * (index % 2 === 0 ? 1.45 : 1.25) * Math.sin(Math.PI * swing) ** 2,
  };
}
