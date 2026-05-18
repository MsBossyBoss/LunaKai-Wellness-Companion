from __future__ import annotations

import argparse

import requests


def main() -> None:
    parser = argparse.ArgumentParser(description="Send a WAV file to LunaKai local faster-whisper.")
    parser.add_argument("--url", default="http://192.168.1.231:8001/transcribe")
    parser.add_argument("--file", required=True)
    args = parser.parse_args()

    with open(args.file, "rb") as audio_file:
        response = requests.post(args.url, files={"file": audio_file}, timeout=180)
    response.raise_for_status()
    print(response.json())


if __name__ == "__main__":
    main()
