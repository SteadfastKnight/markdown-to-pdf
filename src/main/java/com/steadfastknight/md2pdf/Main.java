package com.steadfastknight.md2pdf;

import com.steadfastknight.md2pdf.ui.MainApp;

import java.nio.file.Path;
import java.util.Set;

/**
 * Dispatch, mirroring the Python __main__: no args -> GUI, args -> CLI.
 */
public final class Main {
    private Main() {}

    private static final Set<String> PER_OK = Set.of("1", "2", "4");

    public static void main(String[] args) {
        if (args.length > 0) {
            System.exit(runCli(args));
        } else {
            MainApp.main(args);
        }
    }

    /** CLI parity with the Python run_cli(): {@code <file.md> [font-key] [out.pdf] [per:1|2|4]}. */
    static int runCli(String[] args) {
        String md = args[0];
        String fontKey = (args.length > 1 && FontRegistry.FONTS.containsKey(args[1]))
                ? args[1] : FontRegistry.defaultKey();
        int per = (args.length > 3 && PER_OK.contains(args[3])) ? Integer.parseInt(args[3]) : 4;

        String out;
        try {
            if (args.length > 2) {
                out = args[2];
            } else {
                String text = java.nio.file.Files.readString(Path.of(md));
                String title = Booklet.titleFromMd(text, stem(md));
                out = Path.of(md).toAbsolutePath().getParent()
                        .resolve(Booklet.defaultOutName(title, per)).toString();
            }
            Booklet.GenResult r = Booklet.generate(Path.of(md), fontKey, out, per);
            System.out.printf("OK -> %s  (%d sheets, %d-up, %.0fx%.0f mm, font: %s)%n",
                    out, r.sheets(), per, r.wMm(), r.hMm(), fontKey);
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static String stem(String path) {
        String name = Path.of(path).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
