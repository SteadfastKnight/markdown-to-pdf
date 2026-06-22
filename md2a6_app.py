#!/usr/bin/env python
"""md2a6 - Markdown -> print-ready A6 booklet (4-up on A4), self-contained GUI.

Pick a Markdown file, pick a font, pick where to save, click Generate.
Rendering (Markdown -> HTML -> A6 pages) and imposition (4-up on A4 with inner
cut lines + page numbers) are done entirely in-process with PyMuPDF + the
markdown library. No external browser. Fonts are bundled in ./fonts.
"""
import sys, os, re, io, threading, pathlib
import markdown
import fitz  # PyMuPDF

# --- bundled font registry ---------------------------------------------------
# key shown in the UI -> regular/bold TTFs (in the fonts/ folder) + base size.
FONTS = {
    "EB Garamond (classic serif)": dict(reg="EBGaramond-Regular.ttf", bold="EBGaramond-Bold.ttf", size=8.4),
    "Lora (modern serif)":         dict(reg="Lora-Regular.ttf",       bold="Lora-Bold.ttf",       size=7.6),
    "Source Sans (clean sans)":    dict(reg="SourceSans-Regular.ttf", bold="SourceSans-Bold.ttf", size=7.5),
}

# --- layout constants --------------------------------------------------------
MM = 72 / 25.4
A6_W, A6_H = 105 * MM, 148.5 * MM
ML, MT, MR, MB = 12 * MM, 10 * MM, 5.5 * MM, 10 * MM   # left wide for hole-punch
A4_W, A4_H = 595.276, 841.890
EDGE = 1 * MM          # safety inset from sheet edge (printer unprintable border)
GUTTER = 0.0

# pages-per-sheet -> (cols, rows, rotation). A6/A5/A4 share the sqrt(2) aspect,
# so each page fits with at most a 90 deg turn and no distortion.
#   4 -> 2x2  A6 pages (no turn)
#   2 -> 1x2  A5 pages (turned 90 deg: cut across the middle, rotate each half)
#   1 -> 1x1  A4 page  (no turn)
LAYOUTS = {1: (1, 1, 0), 2: (1, 2, 90), 4: (2, 2, 0)}


def resource_dir():
    """Folder that holds bundled assets (handles PyInstaller one-file mode)."""
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return base


def font_dir():
    return os.path.join(resource_dir(), "fonts")


def title_from_md(md_text, fallback):
    lines = md_text.splitlines()
    first = lines[0] if lines else ""
    title = re.sub(r'[<>:"/\\|?*]', "", first.lstrip("#").strip())
    return title or fallback


# trimmed page size per layout, for the suggested file name
SIZE_LABEL = {1: "A4", 2: "A5", 4: "A6"}


def default_out_name(title, per_sheet):
    return f"{title} - {SIZE_LABEL[per_sheet]} {per_sheet}up.pdf"


def _css(font):
    reg, bold, size = font["reg"], font["bold"], font["size"]
    return f"""
@font-face {{ font-family: Body; src: url({reg}); }}
@font-face {{ font-family: Body; font-weight: bold; src: url({bold}); }}
* {{ font-family: Body; }}
body {{ font-size: {size}pt; line-height: 1.34; color: #111; text-align: justify; }}
h1 {{ font-size: {size*1.6:.2f}pt; font-weight: bold; line-height: 1.15;
      margin: 0 0 2.5mm 0; padding-bottom: 1mm; border-bottom: 0.5pt solid #999; }}
h2 {{ font-size: {size*1.15:.2f}pt; font-weight: bold; margin: 3mm 0 1mm 0; }}
p  {{ margin: 0 0 1.6mm 0; }}
ol, ul {{ margin: 0 0 1.6mm 0; padding-left: 4.5mm; }}
li {{ margin: 0 0 1.2mm 0; }}
""".strip()


def render_a6(md_text, font):
    """Markdown -> A6 PDF (bytes), one logical page per A6 sheet."""
    body = markdown.markdown(md_text, extensions=["extra", "sane_lists", "nl2br"])
    html = (f"<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
            f"<body>{body}</body></html>")
    story = fitz.Story(html=html, user_css=_css(font), archive=fitz.Archive(font_dir()))
    buf = io.BytesIO()
    writer = fitz.DocumentWriter(buf)
    mediabox = fitz.Rect(0, 0, A6_W, A6_H)
    where = fitz.Rect(ML, MT, A6_W - MR, A6_H - MB)
    more = 1
    while more:
        dev = writer.begin_page(mediabox)
        more, _ = story.place(where)
        story.draw(dev)
        writer.end_page()
    writer.close()
    return buf.getvalue()


def impose(a6_bytes, out_path, per_sheet=4):
    """Place rendered pages N-up on A4 with inner cut lines + page numbers."""
    cols, rows, rot = LAYOUTS[per_sheet]
    per = cols * rows
    src = fitz.open(stream=a6_bytes, filetype="pdf")
    out = fitz.open()
    cell_w = (A4_W - 2 * EDGE - (cols - 1) * GUTTER) / cols
    cell_h = (A4_H - 2 * EDGE - (rows - 1) * GUTTER) / rows
    n = src.page_count
    dw = dh = 0
    for start in range(0, n, per):
        sheet = out.new_page(width=A4_W, height=A4_H)
        for i in range(per):
            pno = start + i
            if pno >= n:
                break
            col, row = i % cols, i // cols
            x0 = EDGE + col * (cell_w + GUTTER)
            y0 = EDGE + row * (cell_h + GUTTER)
            sp = src[pno]
            pw, ph = sp.rect.width, sp.rect.height
            # page number, drawn ON the source page (bottom-right) so it turns
            # together with the page in rotated layouts.
            label = str(pno + 1)
            tw = fitz.get_text_length(label, fontname="tiro", fontsize=8)
            sp.insert_text((pw - 5.5 * MM - tw, ph - 4 * MM), label,
                           fontname="tiro", fontsize=8, color=(0.35, 0.35, 0.35))
            # footprint after the optional 90 deg turn
            fw, fh = (ph, pw) if rot in (90, 270) else (pw, ph)
            scale = min(cell_w / fw, cell_h / fh)
            dw, dh = fw * scale, fh * scale
            dx = x0 + (cell_w - dw) / 2
            dy = y0 + (cell_h - dh) / 2
            sheet.show_pdf_page(fitz.Rect(dx, dy, dx + dw, dy + dh), src, pno, rotate=rot)
        # inner cut lines only (no outer border): generalize from cols/rows
        for c in range(1, cols):
            x = EDGE + c * (cell_w + GUTTER)
            sheet.draw_line((x, 0), (x, A4_H), color=(0.6, 0.6, 0.6), width=0.3)
        for r in range(1, rows):
            y = EDGE + r * (cell_h + GUTTER)
            sheet.draw_line((0, y), (A4_W, y), color=(0.6, 0.6, 0.6), width=0.3)
    out.save(out_path, garbage=4, deflate=True)
    return out.page_count, dw, dh


def generate(md_path, font_key, out_path, per_sheet=4):
    md_text = pathlib.Path(md_path).read_text(encoding="utf-8")
    a6 = render_a6(md_text, FONTS[font_key])
    sheets, dw, dh = impose(a6, out_path, per_sheet)
    return sheets, dw / MM, dh / MM


# ============================ GUI ============================================
def run_gui():
    import tkinter as tk
    from tkinter import ttk, filedialog, messagebox

    root = tk.Tk()
    root.title("Markdown → A6 Booklet")
    root.resizable(False, False)
    try:
        root.call("tk", "scaling", 1.25)
    except Exception:
        pass

    PER_OPTIONS = {
        "4 — A6 (four per sheet)": 4,
        "2 — A5 (two per sheet)": 2,
        "1 — A4 (one per sheet)": 1,
    }

    pad = dict(padx=10, pady=6)
    md_var = tk.StringVar()
    font_var = tk.StringVar(value=list(FONTS)[0])
    per_var = tk.StringVar(value=list(PER_OPTIONS)[0])
    out_var = tk.StringVar()
    status = tk.StringVar(value="Select a Markdown file to begin.")

    last_suggest = {"val": ""}  # so we only auto-update an unedited field

    def suggest_out(*_):
        try:
            md = md_var.get()
            if not md:
                return
            # don't clobber a path the user typed/picked themselves
            if out_var.get() and out_var.get() != last_suggest["val"]:
                return
            txt = pathlib.Path(md).read_text(encoding="utf-8")
            title = title_from_md(txt, pathlib.Path(md).stem)
            per = PER_OPTIONS[per_var.get()]
            new = str(pathlib.Path(md).parent / default_out_name(title, per))
            out_var.set(new)
            last_suggest["val"] = new
        except Exception:
            pass

    def pick_md():
        p = filedialog.askopenfilename(title="Choose Markdown file",
                                       filetypes=[("Markdown", "*.md *.markdown *.txt"), ("All files", "*.*")])
        if p:
            md_var.set(p)
            suggest_out()
            status.set("Ready.")

    def pick_out():
        init = out_var.get() or "booklet - A6 4up.pdf"
        p = filedialog.asksaveasfilename(title="Save PDF as", defaultextension=".pdf",
                                         initialfile=os.path.basename(init),
                                         initialdir=os.path.dirname(init) or None,
                                         filetypes=[("PDF", "*.pdf")])
        if p:
            out_var.set(p)

    def do_generate():
        md, out = md_var.get(), out_var.get()
        if not md or not os.path.exists(md):
            messagebox.showerror("Missing input", "Choose a valid Markdown file.")
            return
        if not out:
            messagebox.showerror("Missing output", "Choose where to save the PDF.")
            return
        gen_btn.config(state="disabled")
        status.set("Generating…")

        def work():
            try:
                sheets, w, h = generate(md, font_var.get(), out, PER_OPTIONS[per_var.get()])
                msg = f"Done — {sheets} A4 sheet(s), pages {w:.0f}×{h:.0f} mm. Cut at the inner cross."
                root.after(0, lambda: finish_ok(msg, out))
            except PermissionError:
                root.after(0, lambda: finish_err("Could not save — is the PDF open in a viewer? Close it and retry."))
            except Exception as e:
                root.after(0, lambda e=e: finish_err(f"Error: {e}"))

        threading.Thread(target=work, daemon=True).start()

    def finish_ok(msg, out):
        gen_btn.config(state="normal")
        status.set(msg)
        if messagebox.askyesno("Done", msg + "\n\nOpen the PDF now?"):
            try:
                os.startfile(out)  # Windows
            except AttributeError:
                import subprocess
                subprocess.run(["xdg-open" if sys.platform.startswith("linux") else "open", out])

    def finish_err(msg):
        gen_btn.config(state="normal")
        status.set(msg)
        messagebox.showerror("Failed", msg)

    frm = ttk.Frame(root)
    frm.grid(row=0, column=0, sticky="nsew")

    ttk.Label(frm, text="Markdown file").grid(row=0, column=0, sticky="w", **pad)
    ttk.Entry(frm, textvariable=md_var, width=48).grid(row=0, column=1, **pad)
    ttk.Button(frm, text="Browse…", command=pick_md).grid(row=0, column=2, **pad)

    ttk.Label(frm, text="Font").grid(row=1, column=0, sticky="w", **pad)
    ttk.Combobox(frm, textvariable=font_var, values=list(FONTS), state="readonly",
                 width=46).grid(row=1, column=1, sticky="w", **pad)

    ttk.Label(frm, text="Pages per sheet").grid(row=2, column=0, sticky="w", **pad)
    per_cb = ttk.Combobox(frm, textvariable=per_var, values=list(PER_OPTIONS),
                          state="readonly", width=46)
    per_cb.grid(row=2, column=1, sticky="w", **pad)
    per_cb.bind("<<ComboboxSelected>>", suggest_out)

    ttk.Label(frm, text="Save PDF as").grid(row=3, column=0, sticky="w", **pad)
    ttk.Entry(frm, textvariable=out_var, width=48).grid(row=3, column=1, **pad)
    ttk.Button(frm, text="Browse…", command=pick_out).grid(row=3, column=2, **pad)

    gen_btn = ttk.Button(frm, text="Generate", command=do_generate)
    gen_btn.grid(row=4, column=1, sticky="e", **pad)

    ttk.Separator(frm, orient="horizontal").grid(row=5, column=0, columnspan=3, sticky="ew", padx=10)
    ttk.Label(frm, textvariable=status, foreground="#333", wraplength=560,
              justify="left").grid(row=6, column=0, columnspan=3, sticky="w", **pad)

    root.mainloop()


def run_cli(argv):
    if len(argv) < 2:
        print("Usage: md2a6_app.py <file.md> [font-key] [out.pdf] [per-sheet:1|2|4]")
        print("Fonts:", " | ".join(FONTS))
        return 1
    md = argv[1]
    font_key = argv[2] if len(argv) > 2 and argv[2] in FONTS else list(FONTS)[0]
    per = int(argv[4]) if len(argv) > 4 and argv[4] in ("1", "2", "4") else 4
    if len(argv) > 3:
        out = argv[3]
    else:
        txt = pathlib.Path(md).read_text(encoding="utf-8")
        title = title_from_md(txt, pathlib.Path(md).stem)
        out = str(pathlib.Path(md).parent / default_out_name(title, per))
    sheets, w, h = generate(md, font_key, out, per)
    print(f"OK -> {out}  ({sheets} sheets, {per}-up, {w:.0f}x{h:.0f} mm, font: {font_key})")
    return 0


if __name__ == "__main__":
    # No args -> GUI. Args -> CLI (handy for scripting / testing).
    if len(sys.argv) > 1:
        sys.exit(run_cli(sys.argv))
    else:
        run_gui()
