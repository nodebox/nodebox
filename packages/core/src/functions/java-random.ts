/**
 * java.util.Random, bit for bit, so seeded nodes (random_numbers, shuffle, scatter, …) produce the
 * same sequences as NodeBox 3. Uses BigInt for the 48-bit linear congruential state.
 */
export class JavaRandom {
  private seed: bigint;
  private static readonly MULTIPLIER = 0x5deece66dn;
  private static readonly ADDEND = 0xbn;
  private static readonly MASK = (1n << 48n) - 1n;
  private nextNextGaussian: number | null = null;

  constructor(seed: number | bigint) {
    this.seed = (BigInt.asIntN(64, BigInt(Math.trunc(Number(seed)))) ^ JavaRandom.MULTIPLIER) & JavaRandom.MASK;
  }

  /** NodeBox's MathUtils.randomFromSeed: the seed is multiplied by a billion, with Java long overflow. */
  static fromSeed(seed: number): JavaRandom {
    return new JavaRandom(BigInt.asIntN(64, BigInt(Math.trunc(seed)) * 1000000000n));
  }

  next(bits: number): number {
    this.seed = (this.seed * JavaRandom.MULTIPLIER + JavaRandom.ADDEND) & JavaRandom.MASK;
    // (int)(seed >>> (48 - bits))
    return Number(BigInt.asIntN(32, this.seed >> BigInt(48 - bits)));
  }

  nextInt(bound?: number): number {
    if (bound === undefined) return this.next(32);
    if (bound <= 0) throw new Error("bound must be positive");
    if ((bound & -bound) === bound) {
      // Power of two: (int)((bound * (long)next(31)) >> 31)
      return Number((BigInt(bound) * BigInt(this.next(31))) >> 31n);
    }
    let bits: number;
    let val: number;
    do {
      bits = this.next(31);
      val = bits % bound;
    } while (bits - val + (bound - 1) < 0 || bits - val + (bound - 1) > 0x7fffffff);
    return val;
  }

  nextDouble(): number {
    // (((long)next(26) << 27) + next(27)) * 0x1.0p-53
    const hi = BigInt(this.next(26)) << 27n;
    const lo = BigInt(this.next(27));
    return Number(hi + lo) * 2 ** -53;
  }

  nextFloat(): number {
    return this.next(24) / (1 << 24);
  }

  nextBoolean(): boolean {
    return this.next(1) !== 0;
  }

  nextGaussian(): number {
    if (this.nextNextGaussian !== null) {
      const g = this.nextNextGaussian;
      this.nextNextGaussian = null;
      return g;
    }
    let v1: number, v2: number, s: number;
    do {
      v1 = 2 * this.nextDouble() - 1;
      v2 = 2 * this.nextDouble() - 1;
      s = v1 * v1 + v2 * v2;
    } while (s >= 1 || s === 0);
    const multiplier = Math.sqrt((-2 * Math.log(s)) / s);
    this.nextNextGaussian = v2 * multiplier;
    return v1 * multiplier;
  }

  /** Python-style uniform(a, b) on top of the Java generator. */
  uniform(a: number, b: number): number {
    return a + (b - a) * this.nextDouble();
  }

  /** Collections.shuffle(list, random): swap from the end down. */
  shuffle<T>(list: T[]): T[] {
    for (let i = list.length; i > 1; i--) {
      const j = this.nextInt(i);
      const tmp = list[i - 1];
      list[i - 1] = list[j];
      list[j] = tmp;
    }
    return list;
  }
}
