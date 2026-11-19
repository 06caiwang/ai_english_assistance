"""LangChain 翻译链：一次模型调用就能完成，所以不上图。

链路：PromptTemplate -> LLM -> StrOutputParser -> loads_json
"""
from __future__ import annotations

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import Runnable, RunnableLambda

from .llm import MockChatModel, build_llm, source_name
from .parsing import as_str_list, loads_json

SYSTEM = """你是一位英语词典编辑。用户给出一个英文单词或短语，请输出它的中文释义、音标、词性、一个例句。
严格只输出一个 JSON 对象，不要任何解释文字、不要 markdown 代码块，格式如下：
{{"meaning": "中文释义（含词性前缀）", "phonetic": "/音标/", "example": "英文例句", "pos": "词性"}}"""

PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM),
        ("human", "要翻译的内容：{text}"),
    ]
)


def build_chain(llm: BaseChatModel | None = None) -> Runnable:
    llm = llm or build_llm()
    return PROMPT | llm | StrOutputParser() | RunnableLambda(loads_json)


def translate(text: str, llm: BaseChatModel | None = None) -> dict:
    llm = llm or build_llm()
    chain = build_chain(llm)
    raw = chain.invoke({"text": text})

    meaning = str(raw.get("meaning", "")).strip()
    if not meaning:
        # 解析失败的兜底：不抛错，返回一个能看的结果，前端不至于白屏
        meaning = "（模型未返回有效释义，请重试）"

    return {
        "meaning": meaning,
        "phonetic": str(raw.get("phonetic", "")).strip(),
        "example": str(raw.get("example", "")).strip(),
        "pos": str(raw.get("pos", "")).strip(),
        # 按实际使用的模型判断，而不是看环境变量——测试里注入 Mock 时也准确
        "source": "mock" if isinstance(llm, MockChatModel) else source_name(),
    }
