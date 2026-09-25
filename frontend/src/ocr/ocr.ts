import Tesseract from "tesseract.js";

/** Result of analyzing an uploaded station photo with OCR. */
export interface OcrResult {
  text: string;
  looksLikeStation: boolean;
  detectedPrice: number | null;
}

/**
 * Keyword heuristic: does the OCR text plausibly describe a charging station?
 * (design.md — we can't classify the image itself, so we look at extracted text.)
 */
export function looksLikeStation(text: string): boolean {
  const t = text.toLowerCase();
  const tokens = [
    "kwh",
    "kw",
    "charging",
    "charger",
    "ccs",
    "chademo",
    "type 2",
    "type2",
    "nacs",
    "gb/t",
    "gbt",
    "dc fast",
    "ev ",
    "electric",
    "₱",
    "php",
    "peso",
  ];
  return tokens.some((tok) => t.includes(tok));
}

/**
 * Extracts the first plausible ₱/kWh price from OCR text. Handles patterns like
 * "₱15/kWh", "15.50 php / kwh", "P 12 per kWh".
 */
export function extractPrice(text: string): number | null {
  const cleaned = text.replace(/\s+/g, " ");
  // Prefer a number that appears near a currency symbol or a /kWh unit.
  const patterns = [
    /(?:₱|php|p)\s*([0-9]{1,3}(?:\.[0-9]{1,2})?)/i,
    /([0-9]{1,3}(?:\.[0-9]{1,2})?)\s*(?:₱|php|pesos?)?\s*\/?\s*kwh/i,
  ];
  for (const re of patterns) {
    const m = cleaned.match(re);
    if (m) {
      const val = parseFloat(m[1]);
      if (!Number.isNaN(val) && val > 0 && val < 1000) {
        return val;
      }
    }
  }
  return null;
}

/** Runs Tesseract.js on an image file and returns the heuristic analysis. */
export async function analyzeImage(
  file: File,
  onProgress?: (pct: number) => void,
): Promise<OcrResult> {
  const { data } = await Tesseract.recognize(file, "eng", {
    logger: (m) => {
      if (m.status === "recognizing text" && onProgress) {
        onProgress(Math.round(m.progress * 100));
      }
    },
  });
  const text = data.text ?? "";
  return {
    text,
    looksLikeStation: looksLikeStation(text),
    detectedPrice: extractPrice(text),
  };
}
