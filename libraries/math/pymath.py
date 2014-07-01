def evaluate(func, value):
    return eval(func, {}, {"x" : value})