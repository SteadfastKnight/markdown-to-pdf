# markdown-to-pdf · MD-to-PDF Booklet

Turn a Markdown file into a **print-ready booklet PDF** — multiple small pages
imposed on A4 sheets, with cut lines and page numbers, ready to print, cut, and
slot into a notebook.

Built for printing devotionals/notes as small booklet pages, but works for any
Markdown. Fully **self-contained**: no browser, no network, no system fonts
required — rendering is done in-process and three fonts are bundled.

A **Java 21 + JavaFX** application. The pipeline is pure JVM:

| Stage | Library |
|-------|---------|
| Markdown → HTML | [flexmark-java](https://github.com/vsch/flexmark-java) |
| HTML + CSS → A6 PDF | [openhtmltopdf](https://github.com/danfickle/openhtmltopdf) |
| N-up imposition (cut lines, page numbers) | [Apache PDFBox](https://pdfbox.apache.org/) |
| UI | JavaFX |

> The original Python/PyMuPDF implementation is kept for reference under
> [`legacy/`](legacy/) (run `python legacy/md2a6_app.py`).

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

## Requirements

- **JDK 21** (e.g. [Temurin 21](https://adoptium.net/)). Provides `java` and
  `jpackage`.
- **Maven** — none needed globally; the project ships the **Maven Wrapper**
  (`./mvnw`), which provisions the pinned Maven version automatically.

The output filename's title is taken from the **first line** of the Markdown
file (leading `#` stripped).

---

## Build a portable app (no Java install needed by end users)

```sh
./mvnw -Pdist package
```

This produces a **self-contained app-image** under
`target/dist/MD-to-PDF Booklet/` — a portable folder bundling a trimmed Java
runtime + JavaFX + the fonts (~80 MB, no separate Java install required). Copy it
anywhere and run `MD-to-PDF Booklet.exe`.

> The app-image is per-OS — build it on each platform you want to ship.

### Use it
1. **Source file** — choose your `.md`
2. **Typeface** — pick one of three
3. **Layout** — `4`, `2`, or `1` per sheet (the live preview shows the imposition)
4. **Save as** — defaults to `<title> - A6 <N>up.pdf` next to the input
5. **Generate**

---

## Run / develop from source

```sh
./mvnw package                      # builds the runnable fat jar
java -jar "target/app/md2pdf.jar"   # GUI
java -jar "target/app/md2pdf.jar" sample.md   # CLI
```

`target/app/md2pdf.jar` is a shaded jar with everything (incl. JavaFX) bundled,
launched through a non-`Application` `Launcher` class so JavaFX starts cleanly
from the classpath.

---

## CLI

Passing arguments runs the program in command-line mode; with **no arguments it
opens the GUI**.

```
md2pdf.jar <file.md> [font-key] [out.pdf] [per-sheet:1|2|4]
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
java -jar "target/app/md2pdf.jar" notes.md

# Lora, two A5 pages per sheet, explicit output
java -jar "target/app/md2pdf.jar" notes.md "Lora (modern serif)" booklet.pdf 2

# the packaged app works the same in CLI mode
"target/dist/MD-to-PDF Booklet/MD-to-PDF Booklet.exe" notes.md "Source Sans (clean sans)" out.pdf 1
```

---

## Adding more fonts

1. Drop static `Name-Regular.ttf` and `Name-Bold.ttf` into
   `src/main/resources/fonts/`.
2. Add one line to the `FONTS` map in
   `src/main/java/com/steadfastknight/md2pdf/FontRegistry.java`:
   ```java
   FONTS.put("My Font (label)", new Font("Name-Regular.ttf", "Name-Bold.ttf", 7.8));
   ```
   The size is the body point size (headings scale from it). Tune per font.

---

## Layout / tuning reference

Constants live in
`src/main/java/com/steadfastknight/md2pdf/Layout.java` (and font sizes in
`FontRegistry.java`):

| Constant            | Meaning                                      | Default        |
|---------------------|----------------------------------------------|----------------|
| `A6_W, A6_H`        | rendered page size                           | 105 × 148.5 mm |
| `ML, MT, MR, MB`    | page margins (left wide for hole-punch)      | 12/10/5.5/10 mm|
| `EDGE`              | safety inset from A4 sheet edge              | 1 mm           |
| `GUTTER`            | gap between cells                            | 0 mm           |
| `LAYOUTS`           | pages-per-sheet → (cols, rows, rotation)     | 1/2/4          |
| `Font.size`         | per-font body point size                     | 7.5–8.4 pt     |

The page CSS (justification, heading scale, A6 `@page` box) is built in `Css.java`.

---

## Fonts & licensing

Bundled fonts are under the [SIL Open Font License](https://scripts.sil.org/OFL)
(free to use and redistribute):

- **EB Garamond** — Georg Duffner, Octavio Pardo
- **Lora** — Cyreal
- **Source Sans 3** — Adobe

All cover the Romanian Latin Extended characters (ș ț ă î â, „ " quotes).
