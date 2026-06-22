# markdown-to-pdf

Turn a Markdown file into a **print-ready booklet PDF** — multiple small pages
imposed on A4 sheets, with cut lines and page numbers, ready to print, cut, and
slot into a notebook.

Built for printing devotionals/notes as small booklet pages, but works for any
Markdown. Fully **self-contained**: no browser, no network, no system fonts
required — rendering is done in-process with [PyMuPDF](https://pymupdf.readthedocs.io/)
and the [`markdown`](https://python-markdown.github.io/) library, and three
fonts are bundled.

![pages per sheet: 1 / 2 / 4](https://img.shields.io/badge/layouts-1%20%7C%202%20%7C%204%20up-blue)

---

## What it produces

Pages are rendered at A6 size, then imposed on A4. Because A6, A5, and A4 all
share the same √2 aspect ratio, each layout fits with at most a 90° turn and no
distortion:

| Pages/sheet | Result          | Layout            | Notes                                   |
|-------------|-----------------|-------------------|-----------------------------------------|
| **4**       | four A6 pages   | 2×2               | the default                             |
| **2**       | two A5 pages    | 1×2, turned 90°   | cut across the middle, rotate each half |
| **1**       | one A4 page     | 1×1               | full sheet                              |

Every layout includes:
- **12 mm left margin** for hole-punching / perforation
- **1 mm safety inset** from the sheet edge (printer unprintable border)
- **inner cut lines only** (no wasteful outer border)
- **page numbers** bottom-right (they rotate with the page in 2-up mode)
- justified text, bold headings, full Romanian diacritics (ș ț ă î â, „ " quotes)

---

## Quick start (no install)

Double-click **`dist/MD-to-A6 Booklet.exe`** (Windows, ~36 MB, portable — copy it
anywhere).

1. **Markdown file** — choose your `.md`
2. **Font** — pick one of three
3. **Pages per sheet** — `1`, `2`, or `4`
4. **Save PDF as** — defaults to `<title> - A6 <N>up.pdf` next to the input
5. **Generate**

The output filename's title is taken from the **first line** of the Markdown
file (leading `#` stripped).

> First launch may trigger a Windows SmartScreen warning (unsigned binary) —
> *More info → Run anyway*.

---

## CLI

Passing arguments runs the program in command-line mode; with **no arguments it
opens the GUI**.

```
"MD-to-A6 Booklet.exe" <file.md> [font-key] [out.pdf] [per-sheet:1|2|4]
```

| Argument     | Required | Default                                        |
|--------------|----------|------------------------------------------------|
| `file.md`    | yes      | —                                              |
| `font-key`   | no       | `EB Garamond (classic serif)`                  |
| `out.pdf`    | no       | `<md-title> - A6 <N>up.pdf` next to the input  |
| `per-sheet`  | no       | `4`                                            |

**Font keys** (quote them — they contain spaces):
- `EB Garamond (classic serif)`
- `Lora (modern serif)`
- `Source Sans (clean sans)`

**Examples**
```sh
# defaults: EB Garamond, 4-up, auto-named output
"MD-to-A6 Booklet.exe" example.md

# Lora, two A5 pages per sheet, explicit output
"MD-to-A6 Booklet.exe" example.md "Lora (modern serif)" booklet.pdf 2

# run from source instead of the exe
python md2a6_app.py example.md "Source Sans (clean sans)" out.pdf 1
```

> **Note:** the bundled exe is built `--windowed`, so in CLI mode it runs but
> prints nothing to the console and returns immediately — check for the output
> PDF rather than terminal text. Running from source (`python md2a6_app.py …`)
> prints normally. To get a console-mode exe, drop `--windowed` from the build
> command below.

---

## Run / develop from source

Requires Python 3.9+.

```sh
pip install -r requirements.txt
python md2a6_app.py            # GUI
python md2a6_app.py example.md # CLI
```

Runtime dependencies: `pymupdf`, `markdown` (see `requirements.txt`).

### Adding more fonts

1. Drop static `Name-Regular.ttf` and `Name-Bold.ttf` into `fonts/`.
2. Add one line to the `FONTS` dict at the top of `md2a6_app.py`:
   ```python
   "My Font (label)": dict(reg="Name-Regular.ttf", bold="Name-Bold.ttf", size=7.8),
   ```
   `size` is the body point size (headings scale from it). Tune per font.

Variable fonts won't apply bold in PyMuPDF — instantiate static weights first:
```python
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
for style, wght in (("Regular", 400), ("Bold", 700)):
    f = TTFont("MyFont[wght].ttf")
    instantiateVariableFont(f, {"wght": wght}, inplace=True)
    f.save(f"MyFont-{style}.ttf")
```

---

## Regenerating the .exe

The exe is built with [PyInstaller](https://pyinstaller.org/). From this folder:

```powershell
pip install pyinstaller
pyinstaller --onefile --windowed --add-data "fonts;fonts" `
  --name "MD-to-A6 Booklet" --distpath dist `
  --workpath build_pyi --specpath build_pyi `
  md2a6_app.py
```

- `--onefile` — single portable exe
- `--windowed` — no console window for the GUI (remove for a console/CLI build)
- `--add-data "fonts;fonts"` — bundles the font folder (use `fonts:fonts` with a
  `:` separator on macOS/Linux)

The result is `dist/MD-to-A6 Booklet.exe`. `build_pyi/` is scratch and can be
deleted (it's git-ignored).

---

## Layout / tuning reference

Constants live at the top of `md2a6_app.py`:

| Constant            | Meaning                                      | Default        |
|---------------------|----------------------------------------------|----------------|
| `A6_W, A6_H`        | rendered page size                           | 105 × 148.5 mm |
| `ML, MT, MR, MB`    | page margins (left wide for hole-punch)      | 12/10/5.5/10 mm|
| `EDGE`              | safety inset from A4 sheet edge              | 1 mm           |
| `GUTTER`            | gap between cells                            | 0 mm           |
| `LAYOUTS`           | pages-per-sheet → (cols, rows, rotation)     | 1/2/4          |
| `FONTS[..]["size"]` | per-font body point size                     | 7.5–8.4 pt     |

---

## Fonts & licensing

Bundled fonts are under the [SIL Open Font License](https://scripts.sil.org/OFL)
(free to use and redistribute):

- **EB Garamond** — Georg Duffner, Octavio Pardo
- **Lora** — Cyreal
- **Source Sans 3** — Adobe

All cover the Romanian Latin Extended characters used in the sample.
