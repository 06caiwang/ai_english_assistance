"""FastAPI 入口：两个端点 + 健康检查。不连数据库、不写文件，纯文本进出。"""
from __future__ import annotations

import logging
import time

from fastapi import FastAPI
from fastapi.responses import JSONResponse

from .chain import translate
from .generate_graph import build_generate_graph, run_generate
from .graph import build_graph, run_essay
from .llm import llm_mode
from .schemas import (EssayRequest, EssayResponse, GenerateRequest, GenerateResponse,
                      TranslateRequest, TranslateResponse)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
log = logging.getLogger("api")

app = FastAPI(title="english-ai", version="1.0.0")

# 图只编译一次，checkpoint 复用
GRAPH = build_graph()
GEN_GRAPH = build_generate_graph()


@app.get("/health")
def health():
    return {"status": "ok", "model": llm_mode()}


@app.post("/ai/translate", response_model=TranslateResponse)
def ai_translate(req: TranslateRequest):
    log.info("translate text=%s", req.text[:40])
    return translate(req.text)


@app.post("/ai/essay", response_model=EssayResponse)
def ai_essay(req: EssayRequest):
    started = time.time()
    thread_id = f"essay-{abs(hash(req.content)) % 10_000_000}-{int(started)}"
    try:
        result = run_essay(GRAPH, req.content, thread_id)
    except Exception as exc:  # 图内部已尽量兜底，这里只兜最后一道
        log.exception("essay graph failed")
        return JSONResponse(
            status_code=200,
            content={
                "score": 60,
                "dimensions": {"content": 18, "grammar": 15, "vocabulary": 15, "structure": 12},
                "deductions": [],
                "wordCount": 0,
                "lengthNote": "",
                "level": "及格",
                "suggestions": ["批改服务异常，请稍后重试"],
                "trace": ["error"],
                "source": llm_mode(),
            },
        )

    log.info(
        "essay done in %.1fs score=%s trace=%s",
        time.time() - started,
        result.get("score"),
        "->".join(result.get("trace", [])),
    )
    return {**result, "source": "mock" if llm_mode() == "mock" else "ai"}

@app.post("/ai/essay/generate", response_model=GenerateResponse)
def ai_generate(req: GenerateRequest):
    started = time.time()
    thread_id = f"gen-{abs(hash(req.topic + req.genre)) % 10_000_000}-{int(started)}"
    try:
        result = run_generate(GEN_GRAPH, req.topic, req.genre, req.target_words, req.level, thread_id)
    except Exception:
        log.exception("generate graph failed")
        return JSONResponse(status_code=200, content={
            "title": req.topic, "essay": "生成服务异常，请稍后重试", "outline": [],
            "wordCount": 0, "keyWords": [], "goodSentences": [], "trace": ["error"],
            "source": llm_mode(),
        })
    log.info("generate done in %.1fs words=%s trace=%s",
             time.time() - started, result.get("wordCount"), "->".join(result.get("trace", [])))
    return {**result, "source": "mock" if llm_mode() == "mock" else "ai"}
