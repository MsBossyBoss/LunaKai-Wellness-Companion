from __future__ import annotations

import argparse

import requests


def main() -> None:
    parser = argparse.ArgumentParser(description="Request a LunaKai local voice WAV preview.")
    parser.add_argument("--url", default="http://192.168.1.231:8000/speak/kokoro")
    parser.add_argument("--text", default="Hey, I'm LunaKai.")
    parser.add_argument("--voice", default="af_heart")
    parser.add_argument("--out", default="test.wav")
    args = parser.parse_args()

    response = requests.post(
        args.url,
        json={"text": args.text, "voice_id": args.voice, "speed": 1.0, "format": "wav"},
        timeout=120,
    )
    response.raise_for_status()
    with open(args.out, "wb") as audio_file:
        audio_file.write(response.content)
    print(f"Saved {len(response.content)} bytes to {args.out}")


if __name__ == "__main__":
    main()
