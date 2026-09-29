// Rasterizes the brand SVGs into the PNG icons the PWA manifest references.
// Run: node scripts/gen-icons.mjs
import sharp from "sharp";
import { readFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const publicDir = resolve(here, "..", "public");
const iconsDir = resolve(publicDir, "icons");
mkdirSync(iconsDir, { recursive: true });

const anySvg = readFileSync(resolve(publicDir, "favicon.svg"));
const maskableSvg = readFileSync(resolve(iconsDir, "maskable.svg"));

const jobs = [
  { svg: anySvg, size: 192, out: "pwa-192.png" },
  { svg: anySvg, size: 512, out: "pwa-512.png" },
  { svg: anySvg, size: 180, out: "apple-touch-icon.png" }, // iOS home screen
  { svg: maskableSvg, size: 192, out: "pwa-maskable-192.png" },
  { svg: maskableSvg, size: 512, out: "pwa-maskable-512.png" },
];

for (const { svg, size, out } of jobs) {
  await sharp(svg, { density: 384 })
    .resize(size, size, { fit: "contain", background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toFile(resolve(iconsDir, out));
  console.log(`wrote icons/${out} (${size}x${size})`);
}

// Small favicon.ico-style PNG for the tab.
await sharp(anySvg, { density: 384 }).resize(48, 48).png().toFile(resolve(publicDir, "favicon-48.png"));
console.log("wrote favicon-48.png (48x48)");
