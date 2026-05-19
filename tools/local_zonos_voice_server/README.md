# LunaKai Local Zonos Voice Server

This server is the local expressive voice target for Live Companion. It does not use Gemini, OpenAI, ElevenLabs, PlayHT, Cartesia, Android TextToSpeech, or any paid cloud API.

Kokoro remains the stable fallback in the Android app. Zonos is attempted first only when the user selects Zonos and this server is installed/running.

## Windows setup

```powershell
cd C:\Users\WatsonIvana\AndroidStudioProjects\FancieAICompanion\tools\local_zonos_voice_server
python -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

## Zonos engine install attempt

Zonos is not bundled with the app. Install the local engine separately after the base server dependencies:

```powershell
pip install git+https://github.com/Zyphra/Zonos.git
```

Zonos upstream currently documents Linux/macOS first and says Windows is experimental. If install or import fails, do not mark Zonos working. Record the exact package and error. Zonos may require a compatible Python version, PyTorch/torchaudio, C++ Build Tools, CMake, FFmpeg, CUDA/GPU packages, eSpeak, or Rust depending on the dependency chain.

Known Windows checks from this machine:

- Python 3.14.4 and venv work.
- ffmpeg was not on PATH.
- cl.exe and cmake were not on PATH.
- `pip install git+https://github.com/Zyphra/Zonos.git` installed, but `from zonos.model import Zonos` failed with `ModuleNotFoundError: No module named 'zonos.backbone'`, so this install is not a working Zonos speech engine yet.

## Start server

```powershell
python server.py
```

The server binds to:

```text
0.0.0.0:8002
```

Phone/app URL:

```text
http://192.168.1.231:8002
```

## Health test

```powershell
curl http://192.168.1.231:8002/health
```

Expected when the engine is actually usable:

```json
{"ok":true,"engine":"zonos","status":"running","pipeline_loaded":false}
```

Expected when the server starts but the Zonos engine is not usable:

```json
{"ok":false,"engine":"zonos","status":"setup_required"}
```

## Warmup test

```powershell
Invoke-WebRequest -Uri "http://192.168.1.231:8002/warmup" -Method POST
```

Warmup loads the selected Zonos model. It may download model weights on first run and can be slow on CPU.

## Voice test

```powershell
Invoke-WebRequest -Uri "http://192.168.1.231:8002/speak/zonos" -Method POST -ContentType "application/json" -Body '{"text":"Hey, I''m LunaKai.","voice_id":"default","emotion":"warm","speed":1.0,"format":"wav"}' -OutFile zonos_test.wav
```

Do not treat Zonos as complete unless `/speak/zonos` returns playable WAV audio.
