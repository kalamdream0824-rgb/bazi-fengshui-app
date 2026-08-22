# PDF font

The renderer first looks for `NotoSerifSC-Regular.ttf` in this directory. The production artifact must bundle that OFL-licensed font and its `OFL.txt`.

The local sample generator may fall back to macOS `Songti SC Regular` when the bundled font is absent. That fallback is intentionally rejected on hosts without the font, so production cannot silently emit missing Chinese glyphs.
