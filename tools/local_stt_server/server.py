from __future__ import annotations

import importlib.util
import logging
import os
import tempfile
import time
from functools import lru_cache
from typing import Any

import uvicorn
from fastapi import FastAPI, File, HTTPException, UploadFile

HOST = os.getenv("LUNAKAI_STT_HOST", "0.0.0.0")
PORT = int(os.getenv("LUNAKAI_STT_PORT", "8001"))
WHISPER_MODEL = os.getenv("LUNAKAI_WHISPER_MODEL", "base.en")
WHISPER_DEVICE = os.getenv("LUNAKAI_WHISPER_DEVICE", "cpu")
WHISPER_COMPUTE_TYPE = os.getenv("LUNAKAI_WHISPER_COMPUTE_TYPE", "int8")

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("lunakai-local-stt")

app = FastAPI(title="LunaKai Local STT Server", version="0.1.0")


def _package_available(package: str) -> bool:
    return importlib.util.find_spec(package) is not None


@lru_cache(maxsize=1)
def whisper_model() -> Any:
    from faster_whisper import WhisperModel

    log.info("loading faster-whisper model=%s device=%s compute_type=%s", WHISPER_MODEL, WHISPER_DEVICE, WHISPER_COMPUTE_TYPE)
    return WhisperModel(WHISPER_MODEL, device=WHISPER_DEVICE, compute_type=WHISPER_COMPUTE_TYPE)


@app.get("/health")
def health() -> dict[str, Any]:
    available = _package_available("faster_whisper")
    return {
        "ok": available,
        "engine": "faster-whisper",
        "status": "running" if available else "setup_required",
        "model": WHISPER_MODEL,
        "device": WHISPER_DEVICE,
        "compute_type": WHISPER_COMPUTE_TYPE,
        "error": None if available else "Python package 'faster-whisper' is not installed. Run pip install -r requirements.txt.",
    }


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)) -> dict[str, Any]:
    if not _package_available("faster_whisper"):
        raise HTTPException(status_code=503, detail="faster-whisper is not installed. Run pip install -r requirements.txt.")
    suffix = os.path.splitext(file.filename or "speech.wav")[1] or ".wav"
    start = time.perf_counter()
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_audio:
        temp_path = temp_audio.name
        temp_audio.write(await file.read())
    try:
        segments, _ = whisper_model().transcribe(temp_path, vad_filter=True)
        text = " ".join(segment.text.strip() for segment in segments if segment.text.strip()).strip()
        duration_ms = int((time.perf_counter() - start) * 1000)
        log.info("transcribed chars=%s duration_ms=%s", len(text), duration_ms)
        return {"text": text, "partial": False, "duration_ms": duration_ms}
    except Exception as exc:
        log.exception("transcription failed")
        raise HTTPException(status_code=500, detail=f"Transcription failed: {type(exc).__name__}: {exc}") from exc
    finally:
        try:
            os.remove(temp_path)
        except OSError:
            pass


if __name__ == "__main__":
    uvicorn.run("server:app", host=HOST, port=PORT, reload=False)
