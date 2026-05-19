import argparse
from pathlib import Path

import requests


def main() -> None:
    parser = argparse.ArgumentParser(description="Test LunaKai local Zonos speech endpoint.")
    parser.add_argument("--url", default="http://192.168.1.231:8002/speak/zonos")
    parser.add_argument("--text", default="Hey, I'm LunaKai.")
    parser.add_argument("--voice-id", default="default")
    parser.add_argument("--emotion", default="warm")
    parser.add_argument("--out", default="zonos_test.wav")
    args = parser.parse_args()

    response = requests.post(
        args.url,
        json={
            "text": args.text,
            "voice_id": args.voice_id,
            "emotion": args.emotion,
            "speed": 1.0,
            "format": "wav",
        },
        timeout=120,
    )
    response.raise_for_status()
    Path(args.out).write_bytes(response.content)
    print(f"saved {args.out} bytes={len(response.content)}")


if __name__ == "__main__":
    main()