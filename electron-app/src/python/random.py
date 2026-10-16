"""Random numbers for Python code in NodeBox.

Replaces the standard random module. The numbers come from the NodeBox core,
the same generator the built-in nodes use, through the JS module
_nodebox_random that the host registers (its Random(seed) returns a
WasmRandom). For an integer seed, random(), uniform(), randrange(),
randint(), choice() and shuffle() give the same results as NodeBox 3.
"""

import _nodebox_random


def _seed_value(a):
    if a is None:
        import time
        return int(time.time() * 1000)
    if isinstance(a, int):
        return a
    return hash(a)


class Random:
    def __init__(self, x=None):
        self._random = _nodebox_random.Random(_seed_value(x))

    def seed(self, a=None):
        self._random.seed(_seed_value(a))

    def random(self):
        """The next number in [0, 1)."""
        return self._random.random()

    def uniform(self, a, b):
        return a + (b - a) * self._random.random()

    def randrange(self, start, stop=None, step=1):
        if stop is None:
            if start <= 0:
                raise ValueError("empty range for randrange()")
            return int(self._random.random() * start)
        if step == 1:
            width = stop - start
            if width <= 0:
                raise ValueError("empty range for randrange()")
            return start + int(self._random.random() * width)
        if step > 0:
            n = (stop - start + step - 1) // step
        elif step < 0:
            n = (stop - start + step + 1) // step
        else:
            raise ValueError("zero step for randrange()")
        if n <= 0:
            raise ValueError("empty range for randrange()")
        return start + step * int(self._random.random() * n)

    def randint(self, a, b):
        return self.randrange(a, b + 1)

    def choice(self, seq):
        if not seq:
            raise IndexError("cannot choose from an empty sequence")
        return seq[int(self._random.random() * len(seq))]

    def shuffle(self, x):
        """Shuffles the list in place."""
        for i in range(len(x) - 1, 0, -1):
            j = int(self._random.random() * (i + 1))
            x[i], x[j] = x[j], x[i]

    def sample(self, population, k):
        """k different elements of the population, in random order."""
        pool = list(population)
        n = len(pool)
        if not 0 <= k <= n:
            raise ValueError("sample larger than population")
        result = []
        for i in range(k):
            j = int(self._random.random() * (n - i))
            result.append(pool[j])
            pool[j] = pool[n - i - 1]
        return result


_inst = Random()
seed = _inst.seed
random = _inst.random
uniform = _inst.uniform
randrange = _inst.randrange
randint = _inst.randint
choice = _inst.choice
shuffle = _inst.shuffle
sample = _inst.sample
