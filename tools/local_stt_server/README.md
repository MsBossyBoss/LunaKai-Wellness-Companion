# LunaKai Local STT Server

This server is the local speech-to-text side of the Live Companion pipeline:

`Android microphone WAV -> faster-whisper -> transcript -> Ollama`

It is local and does not use Gemini speech or any paid cloud speech API.

## Windows setup

Use Python 3.11 or 3.12 for the best package compatibility. This repo was verified with the bundled Python 3.12 runtime.

```powershell
cd C:\Users\WatsonIvana\AndroidStudioProjects\FancieAICompanion\tools\local_stt_server
python -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
python server.py
```

On this workstation, default `python` was `3.14.4`. If your default `python` points to Python 3.14 and faster-whisper dependencies fail, create the venv with an installed 3.12 runtime instead:

```powershell
py -3.12 -m venv .venv
```

Codex verified the install using the bundled Python 3.12 runtime:

```powershell
C:\Users\WatsonIvana\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m venv .venv
```

## Windows prerequisites

Verified on this machine:

- `python --version`: Python 3.14.4.
- `python -m venv test_venv`: works.
- `ffmpeg -version`: missing from PATH.
- `where cl`: missing from PATH.
- `where cmake`: missing from PATH.
- `where git`: available at `C:\Program Files\Git\cmd\git.exe`.

The faster-whisper install completed without Microsoft C++ Build Tools when using Python 3.12 and wheel-backed packages. FFmpeg was not required for the WAV upload test because PyAV installed with wheels, but FFmpeg is still recommended for audio conversion/debugging:

```powershell
winget install Gyan.FFmpeg
```

If a future install reports `Microsoft Visual C++ 14.0 or greater is required`, `Microsoft C++ Build Tools required`, `failed building wheel`, `cl.exe not found`, Rust compiler errors, or CMake errors, install Microsoft C++ Build Tools with:

- Desktop development with C++
- MSVC build tools
- Windows SDK
- CMake tools for Windows, if the failing package asks for CMake

The server binds to `0.0.0.0:8001`, so the phone should reach it at:

```text
http://192.168.1.231:8001
```

If the phone cannot reach it, add a Windows Firewall inbound rule for TCP 8001 on the home-base computer. Ollama also needs inbound TCP 11434, and the local voice server needs inbound TCP 8000.

## Health test

```powershell
curl http://192.168.1.231:8001/health
```

## Transcribe test

Record or provide a WAV file, then run:

```powershell
python test_transcribe.py --file C:\path\to\speech.wav
```

The Android app sends multipart form data to:

```text
POST http://192.168.1.231:8001/transcribe
```

Expected response:

```json
{
  "text": "transcribed speech here",
  "partial": false,
  "duration_ms": 1234
}
```

## Model settings

Defaults are intentionally CPU-friendly:

- `LUNAKAI_WHISPER_MODEL=base.en`
- `LUNAKAI_WHISPER_DEVICE=cpu`
- `LUNAKAI_WHISPER_COMPUTE_TYPE=int8`

You can override them before running `python server.py`.
