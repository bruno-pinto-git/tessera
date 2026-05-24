# Tessera — Brand assets

Single source of truth for the Tessera visual identity. Both the React frontend
and the Android app derive their assets from the files in this folder.

## Files

| File | What it is | When to use |
|---|---|---|
| `logo-mark.svg` | The square logo mark (256×256, rounded square) | Launcher icon, in-app logo, social avatar, anywhere the brand needs a single recognisable shape |

## Colours

- **`#2E7D40`** — Tessera forest green (background of the mark)
- **`#FFFFFF`** — Ticket body

Both colours match `frontend/src/index.css` (`--primary`) and
`android/app/src/main/kotlin/com/tessera/android/ui/theme/Color.kt`
(`TesseraForest`). Keep them in sync.

## Derivatives

When you change `logo-mark.svg`, regenerate the platform-specific assets:

**Frontend** — import the SVG directly via Vite. No conversion step needed.

**Android** — two outputs:
1. `android/app/src/main/res/drawable/ic_tessera_logo.xml` (Vector Drawable,
   used inside the app — Welcome screen etc.). The dashed perforation line on
   the SVG is omitted because Android VectorDrawable doesn't support dashed
   strokes natively; the difference is invisible at icon sizes.
2. `android/app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`
   — 48 / 72 / 96 / 144 / 192 px PNG raster, generated via:
   ```
   npx -y sharp-cli -i branding/logo-mark.svg -o ic_launcher.png resize 192
   ```
   (repeat at each density). Saved into the respective `mipmap-*` folder.

## Don't

- Don't embed the logo as inline `<svg>` in components — import the file.
- Don't recolour the mark. If you need a tinted variant, add it to this folder
  with a clear name (`logo-mark-dark.svg`, `logo-mark-mono.svg`).
- Don't crop or stretch the rounded square — it's the recognisable container.
