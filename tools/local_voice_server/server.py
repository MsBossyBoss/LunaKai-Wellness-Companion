from __future__ import annotations

import importlib.util
import io
import logging
import os
import time
from functools import lru_cache
from typing import Any

import numpy as np
import soundfile as sf
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

HOST = os.getenv("LUNAKAI_VOICE_HOST", "0.0.0.0")
PORT = int(os.getenv("LUNAKAI_VOICE_PORT", "8000"))
SAMPLE_RATE = int(os.getenv("LUNAKAI_KOKORO_SAMPLE_RATE", "24000"))
PRELOAD_ON_STARTUP = os.getenv("LUNAKAI_KOKORO_PRELOAD", "true").lower() in {"1", "true", "yes", "on"}
WARMUP_TEXT = os.getenv("LUNAKAI_KOKORO_WARMUP_TEXT", "LunaKai voice ready.")

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("lunakai-local-voice")

app = FastAPI(title="LunaKai Local Voice Server", version="0.1.0")


class SpeakRequest(BaseModel):
    text: str = Field(..., min_length=1)
    voice_id: str = "af_heart"
    speed: float = 1.0
    format: str = "wav"


AVAILABLE_VOICES = {
    "female_default": "af_heart",
    "female_soft": "af_bella",
    "female_warm": "af_sarah",
    "male_default": "am_adam",
    "male_deep": "am_michael",
    "neutral_default": "neutral_default",
}


def _package_available(package: str) -> bool:
    return importlib.util.find_spec(package) is not None


@lru_cache(maxsize=1)
def kokoro_pipeline() -> Any:
    from kokoro import KPipeline

    return KPipeline(lang_code=os.getenv("LUNAKAI_KOKORO_LANG", "a"))


def _kokoro_status() -> tuple[bool, str | None]:
    if not _package_available("kokoro"):
        return False, "Python package 'kokoro' is not installed. Run pip install -r requirements.txt."
    return True, None


@app.get("/health")
def health() -> dict[str, Any]:
    ok, error = _kokoro_status()
    return {
        "ok": ok,
        "engine": "kokoro",
        "status": "running" if ok else "setup_required",
        "pipeline_loaded": kokoro_pipeline.cache_info().currsize > 0,
        "error": error,
    }


@app.post("/warmup")
def warmup() -> dict[str, Any]:
    started_at = time.perf_counter()
    ok, error = _kokoro_status()
    if not ok:
        raise HTTPException(status_code=503, detail=error)
    try:
        audio = synthesize_kokoro_wav(WARMUP_TEXT, AVAILABLE_VOICES["female_default"], 1.0)
        elapsed_ms = int((time.perf_counter() - started_at) * 1000)
        log.info("kokoro warmup complete elapsed_ms=%s audio_bytes=%s", elapsed_ms, len(audio))
        return {
            "ok": True,
            "engine": "kokoro",
            "pipeline_loaded": kokoro_pipeline.cache_info().currsize > 0,
            "elapsed_ms": elapsed_ms,
            "audio_bytes": len(audio),
            "sample_rate": SAMPLE_RATE,
        }
    except Exception as exc:
        log.exception("kokoro warmup failed")
        raise HTTPException(status_code=500, detail=f"Kokoro warmup failed: {type(exc).__name__}: {exc}") from exc

@app.get("/voices")
def voices() -> dict[str, Any]:
    return {
        "default_provider": "kokoro",
        "voices": AVAILABLE_VOICES,
        "xtts": "placeholder",
        "openvoice": "placeholder",
    }


def synthesize_kokoro_wav(text: str, voice_id: str, speed: float) -> bytes:
    try:
        pipeline = kokoro_pipeline()
    except Exception as exc:
        raise RuntimeError(f"Kokoro could not initialize: {type(exc).__name__}: {exc}") from exc
    generator = pipeline(text.strip(), voice=voice_id, speed=speed)
    chunks: list[np.ndarray] = []
    for _, _, audio in generator:
        chunks.append(np.asarray(audio, dtype=np.float32))
    if not chunks:
        raise RuntimeError("Kokoro generated no audio chunks.")
    audio = np.concatenate(chunks)
    wav = io.BytesIO()
    sf.write(wav, audio, SAMPLE_RATE, format="WAV")
    return wav.getvalue()


@app.post("/speak/kokoro")
def speak_kokoro(request: SpeakRequest) -> Response:
    if request.format.lower() != "wav":
        raise HTTPException(status_code=400, detail="Only wav output is supported right now.")
    ok, error = _kokoro_status()
    if not ok:
        raise HTTPException(status_code=503, detail=error)
    try:
        text = request.text.strip()
        log.info("kokoro request chars=%s voice_id=%s speed=%s", len(text), request.voice_id, request.speed)
        audio = synthesize_kokoro_wav(text[:900], request.voice_id, request.speed)
        return Response(content=audio, media_type="audio/wav")
    except Exception as exc:
        log.exception("kokoro synthesis failed")
        raise HTTPException(status_code=500, detail=f"Kokoro synthesis failed: {type(exc).__name__}: {exc}") from exc


@app.post("/speak/xtts")
def speak_xtts(_: SpeakRequest) -> dict[str, Any]:
    raise HTTPException(
        status_code=501,
        detail="XTTS is a local provider placeholder. Install and wire a local XTTS engine before selecting it.",
    )


@app.post("/speak/openvoice")
def speak_openvoice(_: SpeakRequest) -> dict[str, Any]:
    raise HTTPException(
        status_code=501,
        detail="OpenVoice is a local provider placeholder. Install and wire a local OpenVoice engine before selecting it.",
    )



@app.on_event("startup")
def startup_preload() -> None:
    if not PRELOAD_ON_STARTUP:
        return
    started_at = time.perf_counter()
    ok, error = _kokoro_status()
    if not ok:
        log.warning("kokoro preload skipped: %s", error)
        return
    try:
        kokoro_pipeline()
        elapsed_ms = int((time.perf_counter() - started_at) * 1000)
        log.info("kokoro pipeline preloaded elapsed_ms=%s pipeline_loaded=%s", elapsed_ms, kokoro_pipeline.cache_info().currsize > 0)
    except Exception:
        log.exception("kokoro preload failed")

if __name__ == "__main__":
    uvicorn.run("server:app", host=HOST, port=PORT, reload=False)
