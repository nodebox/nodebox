# Fonts

- `DejaVuSans.ttf`, `DejaVuSans-Bold.ttf` — the fallback face the NodeBox 3 Java engine ended up
  with, so that converted `.ndbx` documents outline text the same way. See `LICENSE-DejaVu.txt`.
- `inter-v13-latin_latin-ext-*.woff2` — the editor's own interface font.
- `FiraSans-Regular.woff` — the `default-font` asset of classic NodeBox Live: `g.textPath` reads it
  when a project names no font of its own, so classic projects need it to outline text at all.
  Fira Sans is published under the SIL Open Font License 1.1; the license text is not in this
  directory yet.
