"""两张图与翻译链的离线测试：全部走 Mock 模型，不依赖网络。"""
from app.chain import translate
from app.generate_graph import build_generate_graph, run_generate
from app.graph import build_graph, run_essay
from app.llm import MockChatModel, build_llm, llm_mode

# 86 词：超过 80 词，不触发字数打折，便于断言"干净"的分数
ESSAY = (
    "My hometown is a small city in the south of China. It has changed a lot in the past "
    "ten years. Many new buildings have been built along the river, and the streets are "
    "much wider than before. Besides, a modern library now stands where an old market used "
    "to be. In short, my hometown has become more prosperous, and I am proud of it. I "
    "believe it will be even better in the future, and more young people will choose to "
    "stay and work here."
)


def _graph(force_low_first: bool = False):
    return build_graph(llm=MockChatModel(force_low_first=force_low_first))


def test_normal_path_five_nodes():
    """正常路径：五节点各跑一次，四维齐全且达标，不重试。"""
    result = run_essay(_graph(), ESSAY, "t-normal")

    assert result["dimensions"] == {"content": 24, "grammar": 19, "vocabulary": 18, "structure": 15}
    assert result["score"] == 76            # 四项之和，未触发字数打折
    assert result["wordCount"] == 86
    assert result["level"] == "良好"
    assert len(result["deductions"]) >= 1   # 扣分明细能指到具体句子
    assert result["deductions"][0]["fix"]
    assert len(result["suggestions"]) >= 3
    assert result["trace"] == [
        "extract_points",
        "check_grammar",
        "check_structure",
        "grade",
        "advise",
    ]


def test_retry_branch_runs_grammar_twice():
    """条件边：首轮维度不全 -> 回到语法节点重跑一次 -> 补齐后收尾。"""
    result = run_essay(_graph(force_low_first=True), ESSAY, "t-retry")

    assert result["trace"].count("check_grammar") == 2
    assert "prepare_retry" in result["trace"]
    assert result["trace"][-1] == "advise"
    assert result["score"] == 76


def test_short_essay_gets_discounted():
    """字数规则：不足 30 词按 ×0.8 并封顶 40，且给出说明。"""
    short = "My hometown is small. It changed a lot."
    result = run_essay(_graph(), short, "t-short")

    assert result["wordCount"] < 30
    assert result["score"] <= 40
    assert "不足 30 词" in result["lengthNote"]


def test_retry_only_once():
    """重试上限：即使一直低分，也只回退一次，不会死循环。"""
    class AlwaysLow(MockChatModel):
        def _reply(self, prompt: str) -> str:
            if "评分" in prompt:
                return '{"dimensions": {"grammar": 15}}'
            return super()._reply(prompt)

    result = run_essay(build_graph(llm=AlwaysLow()), ESSAY, "t-loop")

    assert result["trace"].count("prepare_retry") == 1
    assert result["trace"][-1] == "advise"


def test_generate_graph_runs_outline_draft_check_enrich():
    """生成图：大纲 -> 起草 -> 本地校验 -> 补齐生词，条件边不触发扩写。"""
    graph = build_generate_graph(llm=MockChatModel())
    result = run_generate(graph, "My hometown", "narrative", 120, "senior", "g-1")

    assert result["essay"]
    assert len(result["outline"]) == 3
    assert result["wordCount"] > 0
    assert result["trace"][0] == "make_outline"
    assert "validate" in result["trace"]
    assert result["trace"][-1] == "enrich"
    assert result["keyWords"] and result["keyWords"][0]["word"]


def test_generate_graph_expands_when_too_short():
    """生成图的字数不达标，条件边会走一次扩写再校验。"""
    graph = build_generate_graph(llm=MockChatModel())
    # 目标 300 词，Mock 产出的作文明显不够，触发 expand
    result = run_generate(graph, "My hometown", "narrative", 300, "senior", "g-2")

    assert result["trace"].count("validate") >= 2
    assert "adjust" in result["trace"]


def test_generate_graph_shortens_when_too_long():
    """目标词数很小，Mock 初稿偏长，条件边应走"精简"而不是越写越长。"""
    graph = build_generate_graph(llm=MockChatModel())
    result = run_generate(graph, "My hometown", "narrative", 30, "junior", "g-3")

    assert "adjust" in result["trace"]
    assert result["wordCount"] <= 40, "精简后不应该还超出目标太多"


def test_translate_chain():
    out = translate("resilient", llm=MockChatModel())
    assert out["meaning"]
    assert out["phonetic"].startswith("/")
    assert out["source"] == "mock"


def test_llm_falls_back_to_mock_without_key(monkeypatch):
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)
    monkeypatch.delenv("LLM_MODE", raising=False)
    assert llm_mode() == "mock"
    assert isinstance(build_llm(), MockChatModel)
