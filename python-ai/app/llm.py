"""模型工厂：有 Key 走 DeepSeek，没有 Key 走 Mock。

设计要点：Mock 不是"假数据糊弄"，而是让图的拓扑、条件边、checkpoint 全部真跑，
只把模型这一层换掉。这样断网、欠费、忘配 Key 都能演示主链路，也能离线跑单测。
"""
from __future__ import annotations

import json
import os
from typing import Any, List, Optional

from dotenv import load_dotenv
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage
from langchain_core.outputs import ChatGeneration, ChatResult
from pydantic import PrivateAttr

load_dotenv()


#: Mock 生成的样文。用真实换行写，交给 json.dumps 转义——手写 \n 容易漏转义导致 JSON 解析失败
_DRAFT_PARAS = [
    "My hometown is a small city in the south of China.",
    "In the past ten years it has changed a lot. First, many new buildings were built. "
    "Besides, the streets became wider.",
    "In short, I love my hometown and I am proud of its changes.",
]

_EXPANDED_PARAS = [
    "My hometown lies in the south of China. It is a small but peaceful city.",
    "Over the past ten years, it has changed a great deal. First, many new buildings "
    "have been built along the river. Besides, the streets are much wider than before, "
    "so traffic is easier. Moreover, a modern library now stands where an old market "
    "used to be.",
    "In short, my hometown has become more prosperous, and I am proud of it.",
]

_SHORT_PARAS = [
    "My hometown is a small city in the south of China.",
    "In the past ten years, new buildings and wider streets have changed it a lot.",
    "In short, I am proud of my hometown.",
]

_DRAFT = "\n\n".join(_DRAFT_PARAS)
_SHORT = "\n\n".join(_SHORT_PARAS)
_EXPANDED = "\n\n".join(_EXPANDED_PARAS)


class MockChatModel(BaseChatModel):
    """按 prompt 关键词返回结构合法的假 JSON。

    force_low_first=True 时第一次评分返回 52 分（dimensions 缺一个维度），
    用于测试条件边是否真的回到语法节点重跑。
    """

    force_low_first: bool = False

    _score_calls: int = PrivateAttr(default=0)

    @property
    def _llm_type(self) -> str:
        return "mock"

    # ---- 关键词路由 ----
    def _reply(self, prompt: str) -> str:
        """按提示词里的**特征短语**路由，不要用"作文/结构"这种泛词——
        评分提示词里会同时出现"语法""结构"，用泛词会串到别的分支。"""
        if "生词" in prompt:
            return (
                '{"keywords": [{"word": "skyline", "meaning": "n. 天际线"}, '
                '{"word": "prosperous", "meaning": "adj. 繁荣的"}], '
                '"good_sentences": ["What impresses me most is the riverside park."]}'
            )
        if "精简" in prompt:
            return json.dumps({"draft": _SHORT})
        if "扩写" in prompt:
            return json.dumps({"draft": _EXPANDED})
        if "评分规则" in prompt:
            self._score_calls += 1
            if self.force_low_first and self._score_calls == 1:
                return '{"dimensions": {"grammar": 15}}'
            return (
                '{"dimensions": {"content": 24, "grammar": 19, "vocabulary": 18, "structure": 15},'
                ' "deductions": [{"type": "tense", "sentence": "Many new building was built.",'
                ' "fix": "Many new buildings were built.", "points": -2}]}'
            )
        if "写作要点" in prompt:
            return (
                '{"points": ["描述家乡的地理位置", "说明家乡的变化", "表达对家乡的感情"]}'
            )
        if "语法错误" in prompt:
            return (
                '{"grammar_issues": ["第三段第 2 句时态不一致", "存在一处主谓不一致"]}'
            )
        if "段落结构" in prompt:
            return '{"structure_issues": ["缺少明显的过渡句", "结尾过于仓促"]}'
        if "改进建议" in prompt:
            return (
                '{"suggestions": ["在段落之间加过渡句", "统一使用一般过去时", "结尾补一句总结感受"]}'
            )
        if "写作大纲" in prompt:
            return (
                '{"outline": ["引入：家乡的位置与总体印象", "主体：十年间的三点变化", '
                '"结尾：情感升华"]}'
            )
        if "写一篇英语作文" in prompt:
            return json.dumps({"draft": _DRAFT})
        # 默认按翻译处理
        return (
            '{"meaning": "adj. 有弹性的；能迅速恢复的", "phonetic": "/rɪˈzɪliənt/", '
            '"example": "Children are usually more resilient than adults.", '
            '"pos": "adj."}'
        )

    def _generate(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Any = None,
        **kwargs: Any,
    ) -> ChatResult:
        prompt = "\n".join(str(m.content) for m in messages)
        text = self._reply(prompt)
        return ChatResult(generations=[ChatGeneration(message=AIMessage(content=text))])


def llm_mode() -> str:
    """返回当前使用的模型模式：deepseek / mock。"""
    mode = os.getenv("LLM_MODE", "").strip().lower()
    if mode in ("mock", "deepseek"):
        return mode
    return "deepseek" if os.getenv("DEEPSEEK_API_KEY", "").strip() else "mock"


def build_llm(force_low_first: bool = False) -> BaseChatModel:
    """统一入口。测试里可以直接传 force_low_first=True 造出重试分支。"""
    if llm_mode() == "deepseek":
        from langchain_openai import ChatOpenAI

        return ChatOpenAI(
            model=os.getenv("DEEPSEEK_MODEL", "deepseek-chat"),
            base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
            api_key=os.getenv("DEEPSEEK_API_KEY"),
            timeout=10,
            max_retries=0,
            temperature=0.2,
        )
    return MockChatModel(force_low_first=force_low_first)


def source_name() -> str:
    """给 Spring 透传给客户端用：ai / mock。"""
    return "mock" if llm_mode() == "mock" else "ai"
