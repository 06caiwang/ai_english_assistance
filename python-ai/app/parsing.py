"""JSON 清洗：模型经常把 JSON 包在 ```json 代码块里，直接 json.loads 会炸。"""
from __future__ import annotations

import json
import re
from typing import Any, Dict


def loads_json(text: str) -> Dict[str, Any]:
    """尽最大努力从模型输出里抠出一个 JSON 对象；失败返回空 dict，绝不抛异常。"""
    if not text:
        return {}
    s = text.strip()

    # 去掉 ```json ... ``` 包裹
    if "```" in s:
        m = re.search(r"```(?:json)?\s*(.*?)```", s, re.S)
        if m:
            s = m.group(1).strip()

    start, end = s.find("{"), s.rfind("}")
    if start == -1 or end == -1 or end <= start:
        return {}
    try:
        data = json.loads(s[start : end + 1])
        return data if isinstance(data, dict) else {}
    except json.JSONDecodeError:
        return {}


def as_int(value: Any, default: int = 0) -> int:
    try:
        return int(round(float(value)))
    except (TypeError, ValueError):
        return default


def as_str_list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(v).strip() for v in value if str(v).strip()]
    if isinstance(value, str) and value.strip():
        return [value.strip()]
    return []
