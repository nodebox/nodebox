// Encode a number in Base 62
function base62Encode(
  num: number,
  alphabet: string = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
): string {
  if (num === 0) return alphabet[0];
  let arr: string[] = [];
  const base: number = alphabet.length;
  while (num > 0) {
    let rem: number = num % base;
    num = Math.floor(num / base);
    arr.push(alphabet[rem]);
  }
  return arr.reverse().join("");
}

// Generate a semi-readable unique ID
export function generateRandomId(timestampLength: number = 8, randomLength: number = 4): string {
  // Encode current time to base62
  const now: number = new Date().getTime();
  const timestampPart: string = base62Encode(now).slice(-timestampLength);
  // Generate random alphanumeric string
  const characters: string = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let randomPart: string = "";
  for (let i = 0; i < randomLength; i++) {
    randomPart += characters.charAt(Math.floor(Math.random() * characters.length));
  }
  return timestampPart + randomPart;
}

export function checkUserId(id: string): boolean {
  return /^[a-z0-9\-]{3,32}$/.test(id);
}

export function checkProjectId(id: string): boolean {
  return /^[a-z0-9\-]{3,32}\/[a-z0-9\-]{3,32}$/.test(id);
}

export function checkFunctionId(id: string): boolean {
  return /^[a-z0-9\-]{3,32}\/[a-z0-9\-]{1,32}\/[a-zA-Z0-9:\-\s]{2,32}$/.test(id);
}

export function generateUniqueName(prefix: string, existingNames: string[]): string {
  let counter: number = 1;
  let name: string;
  do {
    name = `${prefix} ${counter}`;
    counter++;
  } while (existingNames.includes(name));
  return name;
}
