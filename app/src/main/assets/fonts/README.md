# Shipped font subset

`jrime-ext-subset.ttf` is a subset of
[HanaMinB](https://fonts.google.com/specimen/Hana+Mincho) (花園明朝B, the
Hanazono font, public domain). It carries only the CJK Extension B–G
ideographs that appear in the bundled flyhe dictionaries (about 52k
codepoints, subset with fonttools) — every glyph the system fonts already
draw stays on the system face, and this subset only catches the rare
ideographs the system cannot render.

## Regenerating

After the dictionaries change, re-run the subset so the shipped coverage
follows the dictionary's own characters:

```sh
python3 - <<'EOF'
import re, glob, pathlib
EXT = re.compile(r'[\U00020000-\U0002FA1F]')
chars = set()
for f in glob.glob('app/data/rime/dicts/*.yaml') + \
         glob.glob('app/src/main/assets/shared/*.yaml') + \
         glob.glob('app/src/main/assets/shared/lua/*.lua'):
    chars |= set(EXT.findall(pathlib.Path(f).read_text(encoding='utf-8', errors='ignore')))
open('/tmp/ext-chars.txt', 'w').write('\n'.join('U+%X' % ord(c) for c in sorted(chars)))
EOF

pyftsubset HanaMinB.ttf \
  --unicodes-file=/tmp/ext-chars.txt \
  --output-file=app/src/main/assets/fonts/jrime-ext-subset.ttf \
  --layout-features='*' --name-IDs='*' --name-languages='*' \
  --recalc-bounds --no-hinting
```

Beyond the shipped coverage, the in-app "Import font" entry
(Settings → General → Import font) accepts any ttf/otf/ttc into the user
font directory as an extra fallback layer.
