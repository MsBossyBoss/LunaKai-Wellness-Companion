import io
import logging
import os
import time
from functools import lru_cache
from typing import Any

import soundfile as sf
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

HOST = os.getenv("LUNAKAI_ZONOS_HOST", "0.0.0.0")
PORT = int(os.getenv("LUNAKAI_ZONOS_PORT", "8002"))
DEFAULT_MODEL_REPO = os.getenv("LUNAKAI_ZONOS_MODEL", "Zyphra/Zonos-v0.1-transformer")
MAX_SECONDS = int(os.getenv("LUNAKAI_ZONOS_MAX_SECONDS", "16"))

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("lunakai-zonos-voice")
app = FastAPI(title="LunaKai Local Zonos Voice Server")


class SpeakRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=1200)
    voice_id: str = Field("default")
    emotion: str = Field("warm")
    speed: float = Field(1.0, ge=0.5, le=1.5)
    format: str = Field("wav")


def _load_zonos_modules() -> tuple[Any, Any, Any, Any]:
    from zonos.conditioning import make_cond_dict  # type: ignore
    from zonos.model import Zonos  # type: ignore
    from zonos.utils import DEFAULT_DEVICE  # type: ignore
    import torch  # type: ignore

    return Zonos, make_cond_dict, DEFAULT_DEVICE, torch


def _zonos_import_error() -> str | None:
    try:
        _load_zonos_modules()
        return None
    except Exception as exc:  # pragma: no cover - depends on local install
        return f"{type(exc).__name__}: {exc}"


@lru_cache(maxsize=1)
def _zonos_status() -> tuple[bool, str | None]:
    error = _zonos_import_error()
    if error:
        return False, error
    return True, None


@lru_cache(maxsize=1)
def _load_model() -> Any:
    Zonos, _, device, _ = _load_zonos_modules()
    log.info("loading Zonos model repo=%s device=%s", DEFAULT_MODEL_REPO, device)
    return Zonos.from_pretrained(DEFAULT_MODEL_REPO, device=device)


def _pipeline_loaded() -> bool:
    return _load_model.cache_info().currsize > 0


@app.get("/health")
def health() -> dict[str, Any]:
    ok, error = _zonos_status()
    return {
        "ok": ok,
        "engine": "zonos",
        "status": "running" if ok else "setup_required",
        "detail": None if ok else f"Zonos Python engine is not usable: {error}",
        "model": DEFAULT_MODEL_REPO,
        "pipeline_loaded": _pipeline_loaded() if ok else False,
    }


@app.post("/warmup")
def warmup() -> dict[str, Any]:
    started_at = time.perf_counter()
    ok, error = _zonos_status()
    if not ok:
        raise HTTPException(status_code=503, detail=f"Zonos engine is not usable: {error}")
    try:
        _load_model()
        return {
            "ok": True,
            "engine": "zonos",
            "status": "running",
            "pipeline_loaded": True,
            "elapsed_ms": int((time.perf_counter() - started_at) * 1000),
        }
    except Exception as exc:
        log.exception("zonos warmup failed")
        raise HTTPException(status_code=500, detail=f"Zonos warmup failed: {type(exc).__name__}: {exc}") from exc


@app.post("/speak/zonos")
def speak_zonos(request: SpeakRequest) -> Response:
    started_at = time.perf_counter()
    ok, error = _zonos_status()
    if not ok:
        raise HTTPException(
            status_code=503,
            detail=(
                "Zonos engine is not installed or failed to import. "
                "Install a usable local Zonos package on this computer, then restart this server. "
                f"Import error: {error}"
            ),
        )

    if request.format.lower() != "wav":
        raise HTTPException(status_code=400, detail="Only wav format is supported right now.")

    try:
        wav_bytes = synthesize_zonos_wav(request)
        elapsed_ms = int((time.perf_counter() - started_at) * 1000)
        log.info("zonos speech complete elapsed_ms=%s voice_id=%s bytes=%s", elapsed_ms, request.voice_id, len(wav_bytes))
        return Response(content=wav_bytes, media_type="audio/wav")
    except Exception as exc:
        log.exception("zonos speech failed")
        raise HTTPException(status_code=500, detail=f"Zonos speech failed: {type(exc).__name__}: {exc}") from exc


def _emotion_vector(style: str) -> list[float]:
    normalized = style.strip().lower()
    if normalized in {"happy", "bright", "playful"}:
        return [0.50, 0.03, 0.02, 0.02, 0.10, 0.02, 0.16, 0.15]
    if normalized in {"calm", "soft", "gentle"}:
        return [0.18, 0.04, 0.02, 0.02, 0.03, 0.02, 0.18, 0.51]
    if normalized in {"intense", "dominant", "dramatic"}:
        return [0.22, 0.03, 0.02, 0.04, 0.12, 0.18, 0.27, 0.12]
    return [0.36, 0.03, 0.02, 0.02, 0.07, 0.02, 0.20, 0.28]


def synthesize_zonos_wav(request: SpeakRequest) -> bytes:
    Zonos, make_cond_dict, _, torch = _load_zonos_modules()
    model = _load_model()
    device = model.device
    speaking_rate = max(8.0, min(28.0, 15.0 * request.speed))
    max_new_tokens = 86 * max(2, MAX_SECONDS)

    with torch.inference_mode():
        cond_dict = make_cond_dict(
            text=request.text.strip(),
            language="en-us",
            emotion=_emotion_vector(request.emotion),
            speaking_rate=speaking_rate,
            device=device,
        )
        conditioning = model.prepare_conditioning(cond_dict)
        codes = model.generate(
            conditioning,
            max_new_tokens=max_new_tokens,
            progress_bar=False,
            disable_torch_compile=model.device.type == "cpu",
        )
        wavs = model.autoencoder.decode(codes).detach().cpu().float()

    audio = wavs[0]
    if audio.ndim == 2:
        audio = audio.squeeze(0)
    audio_array = audio.numpy()
    buffer = io.BytesIO()
    sf.write(buffer, audio_array, model.autoencoder.sampling_rate, format="WAV")
    return buffer.getvalue()


if __name__ == "__main__":
    uvicorn.run("server:app", host=HOST, port=PORT, reload=False)
