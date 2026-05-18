# LunaKai Local Voice Server

This server is the local voice output side of the Live Companion pipeline:

`Android app -> Ollama reply text -> local voice server -> WAV audio -> Android playback`

It does not use Android TextToSpeech, Gemini, OpenAI, ElevenLabs, PlayHT, Cartesia, or any paid cloud API.

## Windows setup

Use Python 3.11 or 3.12 for Kokoro package compatibility. This repo was verified with the bundled Python 3.12 runtime.

```powershell
cd C:\Users\WatsonIvana\AndroidStudioProjects\FancieAICompanion\tools\local_voice_server
python -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
python server.py
```

On this workstation, default `python` was `3.14.4`. Kokoro 0.7.16 requires Python `<3.13`, so the exact commands above only work when `python` points to Python 3.11 or 3.12. If your default `python` points to Python 3.13 or 3.14, create the venv with an installed 3.12 runtime instead:

```powershell
py -3.12 -m venv .venv
```

Codex verified the install using the bundled Python 3.12 runtime:

```powershell
C:\Users\WatsonIvana\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m venv .venv
```

## Windows prerequisites

Verified on this machine:

- `python --version`: Python 3.14.4, but Kokoro needs Python 3.11 or 3.12.
- `python -m venv test_venv`: works.
- `ffmpeg -version`: missing from PATH.
- `where cl`: missing from PATH.
- `where cmake`: missing from PATH.
- `where git`: available at `C:\Program Files\Git\cmd\git.exe`.

Kokoro installed without Microsoft C++ Build Tools after switching to Python 3.12 and using wheel-backed dependencies. If a future install reports `Microsoft Visual C++ 14.0 or greater is required`, `Microsoft C++ Build Tools required`, `failed building wheel`, `cl.exe not found`, or CMake errors, install Microsoft C++ Build Tools with:

- Desktop development with C++
- MSVC build tools
- Windows SDK
- CMake tools for Windows, if the failing package asks for CMake

FFmpeg is not required for the Kokoro WAV endpoint, but install it for broader audio debugging and conversion support:

```powershell
winget install Gyan.FFmpeg
```

Open a new terminal after installing FFmpeg and rerun:

```powershell
ffmpeg -version
```

The server binds to `0.0.0.0:8000`, so the phone should reach it at:

```text
http://192.168.1.231:8000
```

If the phone cannot reach it, add a Windows Firewall inbound rule for TCP 8000 on the home-base computer. Ollama also needs inbound TCP 11434, and STT needs inbound TCP 8001.

## Health test

```powershell
curl http://192.168.1.231:8000/health
```

Expected when Kokoro is importable:

```json
{
  "ok": true,
  "engine": "kokoro",
  "status": "running"
}
```

If Kokoro is not installed or cannot initialize, `/health` returns `ok=false` with a setup error. The Android app should show that setup error instead of falling back to robotic TTS.

## Kokoro audio test

```powershell
Invoke-WebRequest -Uri "http://192.168.1.231:8000/speak/kokoro" -Method POST -ContentType "application/json" -Body '{"text":"Hey, I''m LunaKai.","voice_id":"af_heart","speed":1.0,"format":"wav"}' -OutFile test.wav
```

Then play `test.wav` on the computer. A successful response is direct `audio/wav` bytes.

You can also use:

```powershell
python test_voice.py --text "Hey, I'm LunaKai." --voice af_heart --out test.wav
```

## Initial voice IDs

- `female_default`: `af_heart`
- `female_soft`: `af_bella`
- `female_warm`: `af_sarah`
- `male_default`: `am_adam`
- `male_deep`: `am_michael`
- `neutral_default`: placeholder for OpenVoice/local cloning when installed

## Endpoints

- `GET /health`
- `GET /voices`
- `POST /speak/kokoro`
- `POST /speak/xtts`
- `POST /speak/openvoice`

XTTS and OpenVoice endpoints are provider placeholders for local engines. They intentionally return setup errors until those local engines are installed and wired.
