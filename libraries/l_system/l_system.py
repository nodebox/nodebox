from nodebox.graphics import Geometry, Path, Transform

def l_system(shape, position, generations, length, length_scale, angle, angle_scale, thickness_scale, premise, *rules):
    if shape is None:
        p = Path()
        p.rect(0, -length/2, 2, length)
        shape = p.asGeometry()
    # Parse all rules
    rule_map = {}
    for rule_index, full_rule in enumerate(rules):
        if len(full_rule) > 0:
            if len(full_rule) < 3 or full_rule[1] != '=':
                raise ValueError("Rule %s should be in the format A=FFF" % (rule_index + 1))
            rule_key = full_rule[0]
            rule_value = full_rule[2:]
            rule_map[rule_key] = rule_value
    # Expand the rules up to the number of generations
    full_rule = premise
    for gen in xrange(int(round(generations))):
        tmp_rule = ""
        for letter in full_rule:
            if letter in rule_map:
                tmp_rule += rule_map[letter]
            else:
                tmp_rule += letter
        full_rule = tmp_rule
    # Now run the simulation
    g = Geometry()
    stack = []
    angleStack = []
    t = Transform()
    t.translate(position.x, position.y)
    angle = angle
    for letter in full_rule:
        if letter == 'F': # Move forward and draw
            transformed_shape = t.map(shape)
            if isinstance(transformed_shape, Geometry):
                g.extend(transformed_shape)
            elif isinstance(transformed_shape, Path):
                g.add(transformed_shape)
            t.translate(0, -length)
        elif letter == '+': # Rotate right
            t.rotate(angle)
        elif letter == '-': # Rotate left
            t.rotate(-angle)
        elif letter == '[': # Push state (start branch)
            stack.append(Transform(t))
            angleStack.append(angle)
        elif letter == ']': # Pop state (end branch)
            t = stack.pop()
            angle = angleStack.pop()
        elif letter == '"': # Multiply length
            t.scale(1.0, length_scale / 100.0)
        elif letter == '!': # Multiply thickness
            t.scale(thickness_scale / 100.0, 1.0)
        elif letter == ';': # Multiply angle
            angle *= angle_scale / 100.0
        elif letter == '_': # Divide length
            t.scale(1.0, 1.0/(length_scale / 100.0))
        elif letter == '?': # Divide thickness
            t.scale(1.0/(thickness_scale / 100.0), 1.0)
        elif letter == '@': # Divide angle
            angle /= angle_scale / 100.0
    return g
