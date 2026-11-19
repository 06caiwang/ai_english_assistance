"""LangGraph 四步作文批改图。

图结构（与 doc/AI-节点图.md 一一对应）：

    START -> extract_points -> check_grammar -> check_structure -> score
                                                                    |
                                          score >= 60 且三维度齐全 -> advise -> END
                                          否则且 retry < 1        -> prepare_retry -> check_grammar
                                          否则（retry 用尽）      -> advise -> END

这张图存在的唯一理由是 score 后面那条条件边：评分不达线可以带着问题回到语法节点重跑，
而不用整条链从头再来。没有这条边，用 Chain 就够了。
"""
from __future__ import annotations

import logging
import operator
from typing import Annotated, Any, Dict, List, TypedDict

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, StateGraph

from .llm import build_llm
from .parsing import as_int, as_str_list, loads_json

log = logging.getLogger("graph")


class EssayState(TypedDict, total=False):
    content: str
    points: List[str]
    grammar_issues: List[str]
    structure_issues: List[str]
    dimensions: Dict[str, int]
    deductions: List[Dict[str, Any]]
    word_count: int
    length_note: str
    level_text: str
    score: int
    suggestions: List[str]
    retry: int
    trace: Annotated[List[str], operator.add]


JSON_ONLY = "严格只输出一个 JSON 对象，不要解释文字，不要 markdown 代码块。"

#: 评分维度与满分（合计 100）
RUBRIC = {"content": 30, "grammar": 25, "vocabulary": 25, "structure": 20}

RUBRIC_TEXT = (
    "评分维度与满分：内容切题 30 分、语法 25 分、词汇 25 分、结构连贯 20 分，合计 100。\n"
    "档位：优秀 90%+ / 良好 75-89% / 及格 60-74% / 不及格 <60%。\n"
    "硬性扣分：拼写错误每处 -1（上限 -5）、时态错误每处 -2（上限 -6）、"
    "主谓不一致每处 -2（上限 -4）、中式表达每处 -2（上限 -6）。\n"
    "必须给出扣分明细，每条指明原句与改法。"
)


def _word_count(text: str) -> int:
    import re
    return len([w for w in re.split(r"\s+", text.strip()) if w])


def _length_rule(score: int, wc: int) -> tuple[int, str]:
    """字数规则在服务端算，不交给模型——模型数不准，也没法解释。"""
    if wc >= 80:
        return score, ""
    if wc >= 60:
        return round(score * 0.9), f"篇幅不足（{wc} 词，60-79 词区间，总分 ×0.9）"
    if wc >= 30:
        return round(score * 0.8), f"篇幅偏短（{wc} 词，30-59 词区间，总分 ×0.8）"
    return min(40, round(score * 0.8)), f"不足 30 词（{wc} 词），无法构成一篇短文"


def _level_text(score: int) -> str:
    if score >= 90:
        return "优秀"
    if score >= 75:
        return "良好"
    if score >= 60:
        return "及格"
    return "不及格"


def _ask(llm: BaseChatModel, system: str, human: str) -> Dict[str, Any]:
    msg = [SystemMessage(content=system), HumanMessage(content=human)]
    try:
        resp = llm.invoke(msg)
        return loads_json(str(resp.content))
    except Exception as exc:  # 单节点失败不炸整张图
        log.warning("node llm call failed: %s", exc)
        return {}


def build_graph(llm: BaseChatModel | None = None, checkpointer: Any = None):
    llm = llm or build_llm()
    ckpt = checkpointer if checkpointer is not None else MemorySaver()

    # ---------------- 节点 ----------------

    def extract_points(state: EssayState) -> Dict[str, Any]:
        log.info("[graph] node=extract_points retry=%s", state.get("retry", 0))
        data = _ask(
            llm,
            "你是英语写作老师。请从学生的作文中提取 3-5 个写作要点。" + JSON_ONLY
            + ' 格式：{"points": ["...", "..."]}',
            f"作文内容：\n{state['content']}",
        )
        return {"points": as_str_list(data.get("points")), "trace": ["extract_points"]}

    def check_grammar(state: EssayState) -> Dict[str, Any]:
        log.info("[graph] node=check_grammar retry=%s", state.get("retry", 0))
        hint = ""
        if state.get("retry", 0) > 0:
            hint = "\n上一轮评分未达线，请更严格地复查并补充遗漏的问题。"
        data = _ask(
            llm,
            "你是英语语法老师。请检查这篇作文的语法错误，逐条列出。" + JSON_ONLY
            + ' 格式：{"grammar_issues": ["...", "..."]}',
            f"作文内容：\n{state['content']}\n已提取的关键点：{state.get('points', [])}{hint}",
        )
        return {
            "grammar_issues": as_str_list(data.get("grammar_issues")),
            "trace": ["check_grammar"],
        }

    def check_structure(state: EssayState) -> Dict[str, Any]:
        log.info("[graph] node=check_structure retry=%s", state.get("retry", 0))
        data = _ask(
            llm,
            "你是英语写作老师。请检查这篇作文的段落结构与逻辑衔接问题。" + JSON_ONLY
            + ' 格式：{"structure_issues": ["...", "..."]}',
            f"作文内容：\n{state['content']}\n前序检查结果：{state.get('grammar_issues', [])}",
        )
        return {
            "structure_issues": as_str_list(data.get("structure_issues")),
            "trace": ["check_structure"],
        }

    def score(state: EssayState) -> Dict[str, Any]:
        log.info("[graph] node=grade retry=%s", state.get("retry", 0))
        data = _ask(
            llm,
            "你是英语作文阅卷老师。严格按下面的评分规则给分。" + RUBRIC_TEXT + JSON_ONLY
            + ' 格式：{"dimensions": {"content": 0, "grammar": 0, "vocabulary": 0, "structure": 0},'
            + ' "deductions": [{"type": "tense", "sentence": "原句", "fix": "改法", "points": -2}]}'
            + " 注意：dimensions 里填的是该维度的得分（不是百分比），四项满分分别为 30/25/25/20。",
            "作文内容：\n{}\n前序检查结果：{} {}".format(
                state["content"],
                state.get("grammar_issues", []),
                state.get("structure_issues", []),
            ),
        )
        raw_dims = data.get("dimensions") or {}
        dims: Dict[str, int] = {}
        if isinstance(raw_dims, dict):
            for key, full in RUBRIC.items():
                if key in raw_dims:
                    dims[key] = max(0, min(full, as_int(raw_dims.get(key), 0)))

        base = sum(dims.values()) if dims else as_int(data.get("score"), 0)
        wc = _word_count(state["content"])
        score_val, note = _length_rule(base, wc)

        raw_ded = data.get("deductions") or []
        deductions: List[Dict[str, Any]] = []
        if isinstance(raw_ded, list):
            for item in raw_ded[:8]:
                if isinstance(item, dict):
                    deductions.append({
                        "type": str(item.get("type", "")),
                        "sentence": str(item.get("sentence", "")),
                        "fix": str(item.get("fix", "")),
                        "points": as_int(item.get("points"), 0),
                    })

        return {
            "score": score_val,
            "dimensions": dims,
            "deductions": deductions,
            "word_count": wc,
            "length_note": note,
            "level_text": _level_text(score_val),
            "trace": ["grade"],
        }

    def prepare_retry(state: EssayState) -> Dict[str, Any]:
        retry = state.get("retry", 0) + 1
        log.info("[graph] node=prepare_retry -> retry=%s", retry)
        return {"retry": retry, "trace": ["prepare_retry"]}

    def advise(state: EssayState) -> Dict[str, Any]:
        log.info("[graph] node=advise retry=%s", state.get("retry", 0))
        data = _ask(
            llm,
            "你是英语写作老师。请针对这篇作文给出 3 条具体可执行的改进建议。" + JSON_ONLY
            + ' 格式：{"suggestions": ["...", "...", "..."]}',
            "作文内容：\n{}\n分数：{}\n问题清单：{} {}".format(
                state["content"],
                state.get("score", 0),
                state.get("grammar_issues", []),
                state.get("structure_issues", []),
            ),
        )
        suggestions = as_str_list(data.get("suggestions"))
        if not suggestions:
            suggestions = ["建议补充具体例子", "注意时态一致", "段落之间加过渡句"]
        return {"suggestions": suggestions, "trace": ["advise"]}

    # ---------------- 条件边 ----------------

    def route_after_score(state: EssayState) -> str:
        dims = state.get("dimensions") or {}
        complete = len(dims) == 4
        if state.get("score", 0) >= 60 and complete:
            return "advise"
        if state.get("retry", 0) < 1:
            return "retry"
        return "advise"  # 重试用尽，带着现有结果收尾

    # ---------------- 组装 ----------------

    g = StateGraph(EssayState)
    g.add_node("extract_points", extract_points)
    g.add_node("check_grammar", check_grammar)
    g.add_node("check_structure", check_structure)
    g.add_node("grade", score)   # 节点名不能与 state 键重名，故叫 grade
    g.add_node("prepare_retry", prepare_retry)
    g.add_node("advise", advise)

    g.set_entry_point("extract_points")
    g.add_edge("extract_points", "check_grammar")
    g.add_edge("check_grammar", "check_structure")
    g.add_edge("check_structure", "grade")
    g.add_conditional_edges(
        "grade", route_after_score, {"advise": "advise", "retry": "prepare_retry"}
    )
    g.add_edge("prepare_retry", "check_grammar")
    g.add_edge("advise", END)

    return g.compile(checkpointer=ckpt)


def run_essay(graph, content: str, thread_id: str) -> Dict[str, Any]:
    """执行一次批改。checkpoint 用 thread_id 隔离，可中断恢复。"""
    state = {"content": content, "retry": 0, "trace": []}
    result = graph.invoke(state, config={"configurable": {"thread_id": thread_id}})
    return {
        "score": as_int(result.get("score"), 0),
        "dimensions": result.get("dimensions", {}),
        "deductions": result.get("deductions", []),
        "wordCount": result.get("word_count", 0),
        "lengthNote": result.get("length_note", ""),
        "level": result.get("level_text", ""),
        "suggestions": result.get("suggestions", []),
        "trace": result.get("trace", []),
    }
