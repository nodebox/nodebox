// Port from Java's Node.isCompatible()
// Determines if a connection from outputType to inputType is valid.

export function isCompatible(outputType: string, inputType: string): boolean {
  if (outputType === inputType) return true;

  // Anything can connect to string (via toString)
  if (inputType === 'string') return true;

  // int <-> float
  if (outputType === 'int' && inputType === 'float') return true;
  if (outputType === 'float' && inputType === 'int') return true;

  // int/float -> point (x=y=value)
  if ((outputType === 'int' || outputType === 'float') && inputType === 'point') return true;

  // list accepts anything
  if (inputType === 'list') return true;

  // data accepts anything
  if (inputType === 'data') return true;

  // geometry accepts points (wrapped)
  if (outputType === 'point' && inputType === 'geometry') return true;

  return false;
}
