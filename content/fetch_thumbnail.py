import json
import time
from pathlib import Path

import requests
from PIL import Image
from io import BytesIO

BASE_DIR = Path(__file__).resolve().parent
DATA_FILE = BASE_DIR / "data.json"
OUTPUT_DIR = BASE_DIR / "thumbnails"

DELAY = 1.5

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140 Safari/537.36"
}


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    with open(DATA_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    islands = data.get("data", data) if isinstance(data, dict) else data

    print(f"Found {len(islands)} islands")

    for i, island in enumerate(islands, 1):
        code = island.get("code")

        if not code:
            print(f"[{i}] SKIP: no code")
            continue

        output_file = OUTPUT_DIR / f"{code}.webp"

        if output_file.exists():
            print(f"[{i}] SKIP: {code} (already exists)")
            continue

        url = f"https://fortnite.gg/island/{code}"

        try:
            print(f"[{i}] Fetching {code}...")

            response = requests.get(
                url,
                headers=HEADERS,
                timeout=20
            )
            response.raise_for_status()

            # Ambil URL image dari og:image
            from bs4 import BeautifulSoup

            soup = BeautifulSoup(response.text, "html.parser")
            meta = soup.select_one("meta[property='og:image']")

            if not meta or not meta.get("content"):
                print(f"    ERROR: image not found")
                continue

            image_url = meta["content"]

            # Download image
            image_response = requests.get(
                image_url,
                headers=HEADERS,
                timeout=20
            )
            image_response.raise_for_status()

            # Convert → WebP
            image = Image.open(BytesIO(image_response.content))
            image = image.convert("RGB")
            image.save(output_file, "WEBP", quality=90)

            print(f"    OK: {output_file}")

        except Exception as e:
            print(f"    ERROR: {e}")

        time.sleep(DELAY)


if __name__ == "__main__":
    main()