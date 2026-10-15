//! Random numbers that match NodeBox 3.
//!
//! NodeBox 3 drew all of its seeded randomness from `java.util.Random`: the
//! Java nodes directly, and the Python nodes through Jython, whose `random`
//! module is backed by `java.util.Random` as well. Using the same generator
//! here means a seeded NodeBox 3 document renders the same in NodeBox 4.
//!
//! The generator is the 48-bit linear congruential generator specified in the
//! `java.util.Random` API documentation (Knuth, TAOCP vol. 2, 3.2.1).

const MULTIPLIER: u64 = 0x5_DEEC_E66D;
const ADDEND: u64 = 0xB;
const MASK: u64 = (1 << 48) - 1;

/// A generator that produces the same sequence as `java.util.Random`.
#[derive(Clone, Debug)]
pub struct JavaRandom {
    seed: u64,
}

impl JavaRandom {
    /// Equivalent to `new java.util.Random(seed)`.
    pub fn new(seed: i64) -> Self {
        let mut r = JavaRandom { seed: 0 };
        r.set_seed(seed);
        r
    }

    /// The generator NodeBox 3's Java nodes use for a seed port
    /// (`MathUtils.randomFromSeed`): the seed is spread out by multiplying
    /// with 1,000,000,000 (wrapping like a Java `long`).
    pub fn from_node_seed(seed: i64) -> Self {
        JavaRandom::new(seed.wrapping_mul(1_000_000_000))
    }

    /// The generator after `random.seed(seed)` in Jython 2.7, for a seed
    /// that comes from an integer port. Jython receives a port value as a
    /// Python long and reduces it modulo `Long.MAX_VALUE`.
    pub fn from_python_seed(seed: i64) -> Self {
        JavaRandom::new(seed.rem_euclid(i64::MAX))
    }

    /// Equivalent to `Random.setSeed(seed)`.
    pub fn set_seed(&mut self, seed: i64) {
        self.seed = (seed as u64 ^ MULTIPLIER) & MASK;
    }

    /// Advances the generator and returns its top `bits` bits (1 to 32),
    /// as `Random.next(bits)` does.
    fn next(&mut self, bits: u32) -> i32 {
        self.seed = self.seed.wrapping_mul(MULTIPLIER).wrapping_add(ADDEND) & MASK;
        (self.seed >> (48 - bits)) as i32
    }

    /// Equivalent to `Random.nextInt()`.
    pub fn next_int(&mut self) -> i32 {
        self.next(32)
    }

    /// Equivalent to `Random.nextInt(bound)`: uniform in `0..bound`.
    ///
    /// # Panics
    ///
    /// Panics if `bound` is not positive.
    pub fn next_int_bounded(&mut self, bound: i32) -> i32 {
        assert!(bound > 0, "bound must be positive");
        if bound & bound.wrapping_neg() == bound {
            // A power of two: take the high bits.
            return ((bound as i64 * self.next(31) as i64) >> 31) as i32;
        }
        loop {
            let bits = self.next(31);
            let val = bits % bound;
            // Reject the values from the incomplete last block.
            if bits.wrapping_sub(val).wrapping_add(bound - 1) >= 0 {
                return val;
            }
        }
    }

    /// Equivalent to `Random.nextDouble()`: uniform in `[0, 1)`, 53 bits.
    pub fn next_double(&mut self) -> f64 {
        let high = (self.next(26) as i64) << 27;
        let low = self.next(27) as i64;
        (high + low) as f64 * (1.0 / (1u64 << 53) as f64)
    }

    /// Jython's `random.random()`: uniform in `[0, 1)`, built from two
    /// 32-bit draws (27 and 26 bits) the way CPython combines them.
    pub fn python_random(&mut self) -> f64 {
        let a = (self.next(32) as u32 >> 5) as f64;
        let b = (self.next(32) as u32 >> 6) as f64;
        (a * 67_108_864.0 + b) * (1.0 / 9_007_199_254_740_992.0)
    }

    /// Equivalent to `java.util.Collections.shuffle(list, random)`: walks the
    /// list from the back and swaps each position with a random earlier (or
    /// the same) position.
    pub fn shuffle<T>(&mut self, items: &mut [T]) {
        for i in (2..=items.len()).rev() {
            let j = self.next_int_bounded(i as i32) as usize;
            items.swap(i - 1, j);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    // Expected values below come from java.util.Random on OpenJDK 21.

    fn hex(d: f64) -> String {
        format!("{:x}", d.to_bits())
    }

    #[test]
    fn next_int_matches_java() {
        let mut r = JavaRandom::new(0);
        assert_eq!([r.next_int(), r.next_int(), r.next_int()], [-1155484576, -723955400, 1033096058]);
        let mut r = JavaRandom::new(42);
        assert_eq!([r.next_int(), r.next_int(), r.next_int()], [-1170105035, 234785527, -1360544799]);
        let mut r = JavaRandom::new(-7);
        assert_eq!([r.next_int(), r.next_int(), r.next_int()], [1155869324, -423064701, -1403676116]);
        let mut r = JavaRandom::new(i64::MAX);
        assert_eq!([r.next_int(), r.next_int(), r.next_int()], [1155099827, 1887904451, 52699159]);
    }

    #[test]
    fn bounded_and_double_match_java() {
        let cases: [(i64, [i32; 7], [&str; 3]); 3] = [
            (1, [0, 0, 6, 5, 6, 1039119989, 1526301748], ["3f790e549c66e000", "3feed6ab7146d0db", "3fee1360946ebd77"]),
            (123456789, [0, 0, 0, 14, 7, 804921648, 925572887], ["3fd701c427d86cb8", "3fde6c80c0417aae", "3fdf2e4177a6fba2"]),
            (i64::MIN, [0, 1, 0, 8, 61, 641609359, 1678332854], ["3fd55373440b5f04", "3fd8a6f089cefe94", "3fef83d267dcd07a"]),
        ];
        for (seed, bounded, doubles) in cases {
            let mut r = JavaRandom::new(seed);
            for _ in 0..3 {
                r.next_int();
            }
            let got: Vec<i32> = [1, 2, 7, 16, 100, 1 << 30, i32::MAX]
                .iter()
                .map(|&b| r.next_int_bounded(b))
                .collect();
            assert_eq!(got, bounded, "seed {seed}");
            let got: Vec<String> = (0..3).map(|_| hex(r.next_double())).collect();
            assert_eq!(got, doubles, "seed {seed}");
        }
    }

    #[test]
    fn node_seed_and_shuffle_match_nodebox_3() {
        let cases: [(i64, [&str; 4], [usize; 10]); 3] = [
            (0, ["3fe764168ea6ca89", "3fcec9e5b3672e14", "3fe465b93a78ef81", "3fe19d2e10efa128"], [4, 8, 9, 6, 3, 5, 2, 1, 7, 0]),
            (-3, ["3f7ecf42a70c7300", "3fe67a2e6370f93e", "3fb127159ddb1e80", "3fea8f6f96817178"], [7, 8, 2, 4, 3, 6, 9, 5, 0, 1]),
            (9223372036, ["3feb14a523485cd5", "3fe9d439c9203db8", "3fd7aaef921df5dc", "3fc7fc9d799a1394"], [9, 3, 1, 0, 5, 2, 4, 8, 6, 7]),
        ];
        for (seed, doubles, shuffled) in cases {
            let mut r = JavaRandom::from_node_seed(seed);
            let got: Vec<String> = (0..4).map(|_| hex(r.next_double())).collect();
            assert_eq!(got, doubles, "seed {seed}");
            let mut items: Vec<usize> = (0..10).collect();
            JavaRandom::from_node_seed(seed).shuffle(&mut items);
            assert_eq!(items, shuffled, "seed {seed}");
        }
    }

    #[test]
    fn python_random_matches_jython() {
        // random.seed(42); random.random() in Jython 2.7.2.
        let mut r = JavaRandom::from_python_seed(42);
        assert_eq!(hex(r.python_random()), "3fe74833a437fa2b");
        // A negative port seed is reduced modulo Long.MAX_VALUE first.
        let mut a = JavaRandom::from_python_seed(-5);
        let mut b = JavaRandom::new(i64::MAX - 5);
        assert_eq!(a.python_random().to_bits(), b.python_random().to_bits());
    }
}
