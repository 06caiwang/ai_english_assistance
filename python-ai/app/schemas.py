"""出入参 schema（Pydantic v2）。"""
from __future__ import annotations

from typing import Dict, List

from pydantic import BaseModel, Field


class TranslateRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=500)


class TranslateResponse(BaseModel):
    meaning: str
    phonetic: str = ""
    example: str = ""
    pos: str = ""
    source: str = "mock"


class EssayRequest(BaseModel):
    content: str = Field(..., min_length=1, max_length=5000)


class Deduction(BaseModel):
    type: str = ""
    sentence: str = ""
    fix: str = ""
    points: int = 0


class EssayResponse(BaseModel):
    score: int
    dimensions: Dict[str, int]
    deductions: List[Deduction] = []
    wordCount: int = 0
    lengthNote: str = ""
    level: str = ""
    suggestions: List[str]
    trace: List[str] = []
    source: str = "mock"


class GenerateRequest(BaseModel):
    topic: str = Field(..., min_length=1, max_length=100)
    genre: str = Field("narrative", pattern="^(narrative|argument|letter)$")
    target_words: int = Field(120, ge=40, le=300)
    level: str = Field("senior", pattern="^(junior|senior|cet4)$")


class GenerateResponse(BaseModel):
    title: str
    essay: str
    outline: List[str] = []
    wordCount: int = 0
    keyWords: List[Dict[str, str]] = []
    goodSentences: List[str] = []
    trace: List[str] = []
    source: str = "mock"
