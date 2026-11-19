"""LangGraph 作文生成图（第二张图）。

    START -> outline -> draft -> check --+-- 字数达标且三段齐全 --> enrich -> END
                                          +-- 不达标且 retry<1 ---> expand -> check
                                          +-- retry 用尽 ---------> enrich -> END

与批改图同构：多步产出 → 本地规则校验 → 条件边回退一次 → 收尾。
字数由本地统计，不交给模型数——模型数不准，也没法解释。
"""
from __future__ import annotations

import logging
import operator
import re
from typing import Annotated, Any, Dict, List, TypedDict

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, StateGraph

from .llm import build_llm
from .parsing import as_int, as_str_list, loads_json

log = logging.getLogger("gen_graph")

GENRES = {"narrative": "记叙文", "argument": "议论文", "letter": "书信"}
LEVELS = {"junior": "初中", "senior": "高中", "cet4": "四级"}

JSON_ONLY = "严格只输出一个 JSON 对象，不要解释文字，不要 markdown 代码块。"


class GenState(TypedDict, total=False):
    topic: str
    genre: str
    target_words: int
    level: str
    outline: List[str]
    draft: str
    word_count: int
    issues: List[str]
    retry: int
    keywords: List[Dict[str, str]]
    good_sentences: List[str]
    trace: Annotated[List[str], operator.add]


def _ask(llm: BaseChatModel, system: str, human: str) -> Dict[str, Any]:
    try:
        resp = llm.invoke([SystemMessage(content=system), HumanMessage(content=human)])
        return loads_json(str(resp.content))
    except Exception as exc:
        log.warning("gen node failed: %s", exc)
        return {}


def _count(text: str) -> int:
    return len([w for w in re.split(r"\s+", text.strip()) if w])


def _paragraphs(text: str) -> int:
    return len([p for p in re.split(r"\n\s*\n", text.strip()) if p.strip()])



def _split_sentences(para: str) -> List[str]:
    return [x for x in re.split(r"(?<=[.!?])\s+", para.strip()) if x]


def _trim_to_limit(text: str, limit: int) -> str:
    """最后一道兜底：模型总把词数写超，靠 prompt 约束不稳定。

    按句子边界删，优先删中间段（首段点题、末段收尾必须留），
    每段至少保留一句。纯本地操作，不再多花一次模型调用。
    """
    paras = [p.strip() for p in text.split("\n\n") if p.strip()]
    if len(paras) < 2:
        return text

    guard = 0
    while _count("\n\n".join(paras)) > limit and guard < 40:
        guard += 1
        # 候选：中间段优先，其次末段；每段至少留一句
        order = list(range(1, len(paras) - 1)) + ([len(paras) - 1] if len(paras) > 1 else [])
        target_idx = None
        for idx in order:
            if len(_split_sentences(paras[idx])) > 1:
                target_idx = idx
                break
        if target_idx is None:
            break
        sents = _split_sentences(paras[target_idx])[:-1]
        paras[target_idx] = " ".join(sents)
        if not paras[target_idx]:
            paras.pop(target_idx)
    return "\n\n".join(paras)


def build_generate_graph(llm: BaseChatModel | None = None, checkpointer: Any = None):
    llm = llm or build_llm()
    ckpt = checkpointer if checkpointer is not None else MemorySaver()

    def make_outline(state: GenState) -> Dict[str, Any]:
        log.info("[gen] node=make_outline topic=%s", state.get("topic"))
        data = _ask(
            llm,
            "你是英语写作老师。请为主题生成三段式写作大纲，每段一句话。" + JSON_ONLY
            + ' 格式：{"outline": ["第一段...", "第二段...", "第三段..."]}',
            f"主题：{state['topic']}\n体裁：{GENRES.get(state.get('genre', 'narrative'))}"
            f"\n难度：{LEVELS.get(state.get('level', 'senior'))}",
        )
        points = as_str_list(data.get("outline"))
        if not points:
            points = ["引入主题与总体印象", "展开两到三个细节", "总结并升华感受"]
        return {"outline": points[:3], "trace": ["make_outline"]}

    def write_draft(state: GenState) -> Dict[str, Any]:
        log.info("[gen] node=write_draft retry=%s", state.get("retry", 0))
        target = as_int(state.get("target_words"), 120)
        data = _ask(
            llm,
            "你是英语写作老师。请按大纲写一篇英语作文。" + JSON_ONLY
            + ' 格式：{"draft": "全文，段落之间用空行分隔"}'
            + f"\n硬性要求：三段式；每段第一句是主题句；至少 3 个连接词；"
            f"词数 {target} 左右（允许 ±15%）；难度为"
            f"{LEVELS.get(state.get('level', 'senior'))}；不要出现中文。",
            "主题：{}\n体裁：{}\n大纲：{}".format(
                state["topic"], GENRES.get(state.get("genre", "narrative")), state.get("outline", [])
            ),
        )
        text = str(data.get("draft", "")).strip()
        return {"draft": text, "word_count": _count(text), "trace": ["write_draft"]}

    def validate(state: GenState) -> Dict[str, Any]:
        text = state.get("draft", "")
        wc = _count(text)
        paras = _paragraphs(text)
        issues: List[str] = []
        target = as_int(state.get("target_words"), 120)
        low, high = int(target * 0.85), int(target * 1.15)
        if wc < low:
            issues.append(f"字数不足：{wc} 词，需补到 {low}-{high} 词")
        if wc > high:
            issues.append(f"字数超出：{wc} 词，需压到 {high} 词以内")
        if paras < 3:
            issues.append(f"段落不足：{paras} 段")
        log.info("[gen] node=validate words=%s paras=%s issues=%s", wc, paras, issues)
        return {"word_count": wc, "issues": issues, "trace": ["validate"]}

    def expand_draft(state: GenState) -> Dict[str, Any]:
        """字数不对就修一次：短了扩写，长了精简。两个方向共用一个节点，避免图里多一条边。"""
        retry = state.get("retry", 0) + 1
        issues = state.get("issues", [])
        too_long = any("超出" in i for i in issues)
        log.info("[gen] node=adjust retry=%s direction=%s issues=%s",
                 retry, "shorten" if too_long else "expand", issues)

        if too_long:
            target = int(state.get("target_words", 120))
            system = (
                "你是英语写作老师。这篇作文超字数了，请精简。" + JSON_ONLY
                + ' 格式：{"draft": "精简后的全文"}'
                + f"\n硬性要求：全文不超过 {int(target * 1.15)} 词；仍保持三段式；"
                "优先删掉形容词堆砌、重复意思的句子和可有可无的例子，不要删掉每段的主题句。"
            )
            human = "当前问题：{}\n原文：\n{}".format(issues, state.get("draft", ""))
        else:
            system = (
                "你是英语写作老师。请按给出的问题扩写这篇作文，保持三段式与原有观点。" + JSON_ONLY
                + ' 格式：{"draft": "扩写后的全文"}'
            )
            human = "问题：{}\n目标词数：{}\n原文：\n{}".format(
                issues, state.get("target_words", 120), state.get("draft", "")
            )

        data = _ask(llm, system, human)
        text = str(data.get("draft", "")).strip() or state.get("draft", "")
        return {"draft": text, "word_count": _count(text), "retry": retry, "trace": ["adjust"]}

    def enrich(state: GenState) -> Dict[str, Any]:
        log.info("[gen] node=enrich")
        data = _ask(
            llm,
            "你是英语老师。请从这篇作文里挑 4 个生词和 2 个亮点句型。" + JSON_ONLY
            + ' 格式：{"keywords": [{"word": "...", "meaning": "..."}], "good_sentences": ["..."]}',
            "作文：\n" + state.get("draft", ""),
        )
        keywords = data.get("keywords") if isinstance(data.get("keywords"), list) else []
        clean_kw = [
            {"word": str(k.get("word", "")), "meaning": str(k.get("meaning", ""))}
            for k in keywords[:6] if isinstance(k, dict) and k.get("word")
        ]
        return {
            "keywords": clean_kw,
            "good_sentences": as_str_list(data.get("good_sentences"))[:3],
            "trace": ["enrich"],
        }

    def route_after_check(state: GenState) -> str:
        if not state.get("issues"):
            return "enrich"
        if state.get("retry", 0) < 2:
            return "expand"
        return "enrich"

    g = StateGraph(GenState)
    g.add_node("make_outline", make_outline)
    g.add_node("write_draft", write_draft)
    g.add_node("validate", validate)
    g.add_node("adjust", expand_draft)
    g.add_node("enrich", enrich)

    g.set_entry_point("make_outline")
    g.add_edge("make_outline", "write_draft")
    g.add_edge("write_draft", "validate")
    g.add_conditional_edges(
        "validate", route_after_check, {"enrich": "enrich", "expand": "adjust"}
    )
    g.add_edge("adjust", "validate")
    g.add_edge("enrich", END)

    return g.compile(checkpointer=ckpt)


def run_generate(graph, topic: str, genre: str, target_words: int, level: str,
                 thread_id: str) -> Dict[str, Any]:
    state = {
        "topic": topic, "genre": genre, "target_words": target_words, "level": level,
        "retry": 0, "trace": [],
    }
    result = graph.invoke(state, config={"configurable": {"thread_id": thread_id}})
    text = result.get("draft", "")
    trace = list(result.get("trace", []))

    high = int(target_words * 1.15)
    if _count(text) > high:
        trimmed = _trim_to_limit(text, high)
        if trimmed != text:
            log.info("[gen] trim %s -> %s words", _count(text), _count(trimmed))
            text = trimmed
            trace.append("trim")
    return {
        "title": topic,
        "essay": text,
        "outline": result.get("outline", []),
        "wordCount": _count(text),
        "keyWords": result.get("keywords", []),
        "goodSentences": result.get("good_sentences", []),
        "trace": trace,
    }
