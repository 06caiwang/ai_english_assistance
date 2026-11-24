"""接口冒烟：单账号（demo01）+ 新规则（答题掉饥饿、三题型、作文生成）。

用法：python tools/smoke.py [base_url]

设计要点：
- 不再每次注册新账号，直接用演示账号跑，跑完数据库仍然只有一个账号；
- 断言用"相对变化"（金币增减、饥饿下降）而不是写死绝对值，可重复执行；
- 会改变数据（金币、饥饿、库存、解锁），跑完如果需要干净状态，执行 db/reset_demo.sql。
"""
from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://127.0.0.1:8080"
USERNAME = "demo01"
PASSWORD = "demo2026"
RESULTS = []


def call(method: str, path: str, body=None, token: str | None = None):
    req = urllib.request.Request(BASE + path,
                                 data=json.dumps(body).encode() if body is not None else None,
                                 method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=90) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw or "{}")
        except json.JSONDecodeError:
            return e.code, {"raw": raw}
    except Exception as e:
        return 0, {"error": str(e)}


def check(name: str, ok: bool, detail: str = ""):
    RESULTS.append((name, ok, detail))
    print(("  PASS  " if ok else "  FAIL  ") + name + ("" if ok else f"   <- {detail}"))
    return ok


def main():
    print(f"BASE = {BASE}  账号 = {USERNAME}\n")

    st, r = call("GET", "/api/ping")
    check("GET /api/ping", r.get("code") == 0, str(r))

    st, r = call("GET", "/api/pet")
    check("无 token 访问返回 401", st == 401 or r.get("code") == 401, f"http={st}")

    st, r = call("POST", "/api/auth/login", {"username": USERNAME, "password": PASSWORD})
    token = (r.get("data") or {}).get("token")
    check("POST /api/auth/login 单账号登录", r.get("code") == 0 and bool(token), str(r))
    if not token:
        summary()
        return

    st, r = call("POST", "/api/auth/login", {"username": USERNAME, "password": "wrongpass"})
    check("错误密码返回 4001", r.get("code") == 4001, str(r))

    st, r = call("GET", "/api/user/me", token=token)
    me = r.get("data") or {}
    coin0 = me.get("coin", 0)
    check("GET /api/user/me", r.get("code") == 0 and me.get("username") == USERNAME, str(me))

    # ---------- 今日单词：带题型与缺空提示 ----------
    st, r = call("GET", "/api/words/today", token=token)
    words = r.get("data") or []
    check("GET /api/words/today 返回 10 条", r.get("code") == 0 and len(words) == 10,
          f"count={len(words)}")
    if not words:
        summary()
        return

    w = words[0]
    check("单词带 modes 与 fillHint",
          bool(w.get("modes")) and len(w.get("fillHint", "")) == len(w.get("spelling", "")),
          str(w))
    check("新词默认三关", w.get("modes") == ["know", "fill", "spell"], str(w.get("modes")))
    hint = w.get("fillHint", "")
    check("缺空提示保留首尾字母",
          hint[0] == w["spelling"][0] and hint[-1] == w["spelling"][-1] and "_" in hint, hint)

    # ---------- 三题型：答题消耗饥饿 ----------
    st, pet_before = call("GET", "/api/pet", token=token)
    hunger0 = (pet_before.get("data") or {}).get("hunger", 0)

    st, r = call("POST", "/api/words/answer",
                 {"wordId": w["wordId"], "mode": "know", "correct": True}, token=token)
    d = r.get("data") or {}
    check("认识：+1 金币 / +2 经验 / 饥饿 -2",
          r.get("code") == 0 and d.get("coinGained") == 1 and d.get("expGained") == 2
          and d.get("hunger") == hunger0 - 2, str(d))

    st, r = call("POST", "/api/words/answer",
                 {"wordId": w["wordId"], "mode": "fill", "answer": "wronganswer"}, token=token)
    d = r.get("data") or {}
    check("缺空默写答错：不给金币、阶段归零",
          r.get("code") == 0 and d.get("correct") is False and d.get("coinGained") == 0
          and d.get("stage") == 0, str(d))

    st, r = call("POST", "/api/words/answer",
                 {"wordId": w["wordId"], "mode": "fill", "answer": w["spelling"]}, token=token)
    d = r.get("data") or {}
    check("缺空默写答对：+1 金币 / +3 经验",
          r.get("code") == 0 and d.get("correct") is True and d.get("coinGained") == 1
          and d.get("expGained") == 3, str(d))

    st, r = call("POST", "/api/words/answer",
                 {"wordId": w["wordId"], "mode": "spell", "answer": w["spelling"].upper()},
                 token=token)
    d = r.get("data") or {}
    check("完整默写答对（忽略大小写）：+2 金币 / +5 经验",
          r.get("code") == 0 and d.get("coinGained") == 2 and d.get("expGained") == 5, str(d))
    check("走完最后一关才推进阶段", d.get("stage") == 1, f"stage={d.get('stage')}")

    # ---------- 喂食 ----------
    st, r = call("GET", "/api/bag", token=token)
    bag = r.get("data") or {}
    foods = bag.get("foods") or []
    check("GET /api/bag 有食物和形象", r.get("code") == 0 and len(foods) >= 1
          and len(bag.get("characters") or []) >= 1, str(bag))

    if foods:
        fid = foods[0]["foodId"]
        before = foods[0]["count"]
        st, r = call("POST", "/api/pet/feed", {"foodId": fid}, token=token)
        d = r.get("data") or {}
        check("POST /api/pet/feed 恢复饥饿并扣库存",
              r.get("code") == 0 and d.get("restored", 0) > 0
              and d.get("foodCount") == before - 1, str(d))

    st, r = call("POST", "/api/pet/feed", {"foodId": 999}, token=token)
    check("喂不存在的食物返回 4001", r.get("code") == 4001, str(r))

    # ---------- 食物与商城 ----------
    st, r = call("GET", "/api/foods", token=token)
    check("GET /api/foods 返回 4 种", r.get("code") == 0 and len(r.get("data") or []) == 4, str(r))

    st, r = call("POST", "/api/foods/buy", {"foodId": 1, "count": 2}, token=token)
    d = r.get("data") or {}
    check("POST /api/foods/buy 扣币加库存",
          r.get("code") == 0 and d.get("spent", 0) > 0, str(d))

    st, r = call("POST", "/api/foods/buy", {"foodId": 1, "count": 0}, token=token)
    check("数量 0 返回 4001", r.get("code") == 4001, str(r))

    # ---------- 形象 ----------
    st, r = call("GET", "/api/characters", token=token)
    chars = r.get("data") or []
    check("GET /api/characters 返回 6 个", r.get("code") == 0 and len(chars) == 6,
          f"count={len(chars)}")

    # ---------- AI ----------
    st, r = call("POST", "/api/ai/translate", {"text": "resilient"}, token=token)
    tr = r.get("data") or {}
    check("POST /api/ai/translate", r.get("code") == 0 and bool(tr.get("meaning")), str(r))
    print(f"          -> [{tr.get('source')}] {tr.get('meaning')}")

    essay = ("My hometown is a small city in the south of China. It has changed a lot in the "
             "past ten years. Many new buildings have been built along the river, and the "
             "streets are much wider than before. Besides, a modern library now stands where "
             "an old market used to be. In short, my hometown has become more prosperous, "
             "and I am proud of it. I believe it will be even better in the future, and more "
             "young people will choose to stay and work here.")
    st, r = call("POST", "/api/ai/essay", {"content": essay}, token=token)
    es = r.get("data") or {}
    check("POST /api/ai/essay 四维分数 + 扣分明细",
          r.get("code") == 0 and len(es.get("dimensions") or {}) == 4
          and es.get("wordCount", 0) > 0 and "deductions" in es, str(es)[:200])
    if es:
        print(f"          -> score={es.get('score')} 档位={es.get('level')} "
              f"词数={es.get('wordCount')} 扣分={len(es.get('deductions') or [])}条")

    st, r = call("POST", "/api/ai/essay/generate",
                 {"topic": "My hometown", "genre": "narrative", "targetWords": 120,
                  "level": "senior"}, token=token)
    gen = r.get("data") or {}
    check("POST /api/ai/essay/generate 返回作文与大纲",
          r.get("code") == 0 and bool(gen.get("essay")) and len(gen.get("outline") or []) == 3,
          str(r)[:200])
    if gen:
        print(f"          -> {gen.get('wordCount')} 词，生词 {len(gen.get('keyWords') or [])} 个，"
              f"轨迹 {'->'.join(gen.get('trace') or [])}")

    # ---------- 历史记录 ----------
    # 同一个词再查一次，验证去重（历史里只应出现一条）
    call("POST", "/api/ai/translate", {"text": "resilient"}, token=token)

    st, r = call("GET", "/api/history/summary", token=token)
    summ = r.get("data") or {}
    check("GET /api/history/summary 三类计数",
          r.get("code") == 0 and summ.get("translate", 0) >= 1
          and summ.get("essay", 0) >= 1 and summ.get("generate", 0) >= 1, str(summ))

    st, r = call("GET", "/api/history/translate?page=1&size=20", token=token)
    page = r.get("data") or {}
    items = page.get("items") or []
    same = [i for i in items if i.get("text") == "resilient"]
    check("GET /api/history/translate 有记录", r.get("code") == 0 and len(items) >= 1,
          str(r)[:160])
    check("同一个词重复查询只留一条（去重）", len(same) == 1, f"命中 {len(same)} 条")
    check("历史单词带释义与时间",
          bool(items[0].get("meaning")) and bool(items[0].get("createdAt")),
          str(items[0])[:160] if items else "")

    st, r = call("GET", "/api/history/essay?page=1&size=5", token=token)
    page = r.get("data") or {}
    items = page.get("items") or []
    check("GET /api/history/essay 有分数与四维",
          r.get("code") == 0 and items and items[0].get("score", 0) > 0
          and bool(items[0].get("dimsJson")), str(items[0])[:160] if items else str(r)[:160])

    st, r = call("GET", "/api/history/generate?page=1&size=5", token=token)
    page = r.get("data") or {}
    items = page.get("items") or []
    check("GET /api/history/generate 有主题与字数",
          r.get("code") == 0 and items and bool(items[0].get("topic"))
          and items[0].get("wordCount", 0) > 0, str(items[0])[:160] if items else str(r)[:160])

    st, r = call("GET", "/api/history/translate?page=1&size=999", token=token)
    check("size 超上限自动收敛到 50", (r.get("data") or {}).get("size") == 50, str(r)[:120])

    st, r = call("GET", "/api/history/translate")
    check("历史接口无 token 返回 401", st == 401 or r.get("code") == 401, f"http={st}")

    # ---------- 统计 ----------
    st, r = call("GET", "/api/stats", token=token)
    check("GET /api/stats", r.get("code") == 0
          and (r.get("data") or {}).get("learnedCount", 0) > 0, str(r))

    summary()


def summary():
    total, passed = len(RESULTS), sum(1 for _, ok, _ in RESULTS if ok)
    print(f"\n===== {passed}/{total} 通过 =====")
    failed = [r for r in RESULTS if not r[1]]
    if failed:
        print("未通过：")
        for name, _, detail in failed:
            print(f"  - {name}  <- {detail[:200]}")
        sys.exit(1)


if __name__ == "__main__":
    main()
