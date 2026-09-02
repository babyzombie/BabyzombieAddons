#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""将 git-cliff 生成的中文更新日志翻译成英文 / 中英双语版本。

用法:
    python3 scripts/translate_changelog.py [changelog.md] [--en changelog-en.md] [--bilingual changelog-bilingual.md] [--mock]

输出:
    changelog-en.md          纯英文版（备用）
    changelog-bilingual.md   双语版，英文在上、中文在下（GitHub Release / Gitee / Modrinth 统一使用）

依赖:
    需要环境变量 DEEPSEEK_API_KEY（DeepSeek 的 OpenAI 兼容接口）。
    未配置或调用失败时回退为中文输出并打印 warning，不中断流程。
    模型默认 deepseek-v4-flash，可用 DEEPSEEK_MODEL 环境变量覆盖（如 deepseek-v4-pro）。

结构说明:
    group 标题（### 新增/优化/修复/其他）走本地映射表，不消耗 API；
    只有 commit 行内容调用 DeepSeek 逐行翻译，并要求保持行数一致。
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except AttributeError:
    pass

API_URL = "https://api.deepseek.com/chat/completions"
# deepseek-v4-flash 实际默认带思考流(响应里会出现 reasoning_content),翻译任务不需要,
# 已在 translate_with_deepseek 中用 thinking: {"type": "disabled"} 显式关闭,防思考吃光 max_tokens 预算。
MODEL = os.environ.get("DEEPSEEK_MODEL", "deepseek-v4-flash")

GROUP_MAP = {
    "新增": "Added",
    "优化": "Improved",
    "修复": "Fixed",
    "其他": "Other",
}

SYSTEM_PROMPT = (
    "You are a professional translator for Minecraft mod changelogs. "
    "Translate each line from Simplified Chinese to English. "
    "Output exactly one translated line per input line, keeping the line count identical "
    "and separating lines with real line breaks - never return a JSON array, "
    "never escape newlines as literal backslash-n characters, never wrap output in code fences. "
    "Preserve any leading symbol such as +, - or * on a line. "
    "Keep mod-specific terms, feature names and proper nouns (e.g. Kuudra, Glacite, Slayer, HUD) as-is. "
    "If a line is already in English or contains only URLs/code, keep it unchanged. "
    "Do not add numbering, bullets, explanations, or any extra text. "
    "Output only the translated lines."
)


def parse_changelog(text):
    """解析为 [(group_title, [commit_lines]), ...]。group 标题为 '### xxx' 原样。"""
    sections = []
    current = None
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("###"):
            current = [stripped, []]
            sections.append(current)
        elif stripped and current is not None:
            current[1].append(line.rstrip())
    return sections


def translate_with_deepseek(lines, api_key):
    """一次调用翻译所有 commit 行，返回与输入等长的英文行列表。"""
    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": "\n".join(lines)},
        ],
        "temperature": 0.2,
        # 思考与输出共用 max_tokens 预算:实测 4 行翻译思考就要烧 ~2800 token,
        # commit 一多思考吃光预算后输出被截断为空,导致"返回 1 行"。翻译不需要推理,
        # 显式关闭;预算留足给长 commit 列表(实测约 10 token/行,8192 可覆盖数百行)。
        "max_tokens": 8192,
        "stream": False,
        "thinking": {"type": "disabled"},
    }
    req = urllib.request.Request(
        API_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {api_key}"},
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    content = data["choices"][0]["message"]["content"]
    return content.rstrip("\n").split("\n")


def translate_lines(lines, api_key, mock=False):
    """翻译 commit 行列表；mock 模式返回 'EN|' 前缀假译文（供本地测试）。"""
    if mock:
        return ["EN| " + line.strip() for line in lines]
    if not lines:
        return []
    translated = translate_with_deepseek(lines, api_key)
    if len(translated) != len(lines):
        # 严重异常(只回一行 / 丢失过半):整体回退,由 main 输出中文版本,不再单行拼凑
        if len(translated) == 1 or len(translated) < max(2, len(lines) // 2):
            print(f"::warning::DeepSeek 返回 {len(translated)} 行，期望 {len(lines)} 行，"
                  "严重异常，整体回退为中文输出", file=sys.stderr)
            return None
        # 轻微偏差(±几行):丢弃多余行 / 用原文补齐缺失行，保证对齐不崩
        print(f"::warning::DeepSeek 返回 {len(translated)} 行，期望 {len(lines)} 行，按行数对齐修正",
              file=sys.stderr)
        result = []
        for i, line in enumerate(lines):
            result.append(translated[i] if i < len(translated) and translated[i].strip() else line)
        return result
    return translated


def local_group_title(title):
    """'### 新增' -> '### Added'；未知分组保留原文。"""
    for zh, en in GROUP_MAP.items():
        if zh in title:
            return title.replace(zh, en, 1)
    return title


def render_en(sections):
    out = []
    for title, lines in sections:
        out.append(local_group_title(title))
        out.extend(lines)
    return "\n".join(out) + "\n"


def render_bilingual(sections, originals):
    """英文在上、中文在下对排。originals 是翻译前的原始中文行（sections 里已被替换成英文）。"""
    out = []
    idx = 0
    for title, lines in sections:
        en_title = local_group_title(title)
        out.append(f"{en_title}（{title[4:]}）")
        for line in lines:
            out.append(line)
            out.append("  " + originals[idx].strip())
            idx += 1
    return "\n".join(out) + "\n"


def main():
    parser = argparse.ArgumentParser(description="git-cliff 中文更新日志翻译脚本")
    parser.add_argument("input", nargs="?", default="changelog.md",
                        help="git-cliff 输出的中文 changelog（默认 changelog.md）")
    parser.add_argument("--en", default="changelog-en.md", help="英文输出路径")
    parser.add_argument("--bilingual", default="changelog-bilingual.md", help="双语输出路径")
    parser.add_argument("--mock", action="store_true", help="本地测试模式：不调用 API，生成假译文")
    args = parser.parse_args()

    api_key = os.environ.get("DEEPSEEK_API_KEY", "")

    with open(args.input, encoding="utf-8", newline="\n") as f:
        text = f.read()

    sections = parse_changelog(text)
    original_lines = [line for _, lines in sections for line in lines]
    commit_lines = list(original_lines)
    print(f"解析到 {len(sections)} 个分组，{len(commit_lines)} 条 commit")

    if not commit_lines:
        print("没有可翻译的 commit 行，直接复制原文")
        content = text
        with open(args.en, "w", encoding="utf-8", newline="\n") as f:
            f.write(content)
        with open(args.bilingual, "w", encoding="utf-8", newline="\n") as f:
            f.write(content)
        return

    if not api_key and not args.mock:
        print("::warning::未设置 DEEPSEEK_API_KEY，跳过翻译，输出中文版本", file=sys.stderr)
        with open(args.en, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        with open(args.bilingual, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        return

    try:
        translated = translate_lines(commit_lines, api_key, mock=args.mock)
    except (urllib.error.URLError, urllib.error.HTTPError, KeyError, json.JSONDecodeError) as e:
        print(f"::warning::DeepSeek 翻译失败（{e}），回退为中文输出", file=sys.stderr)
        with open(args.en, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        with open(args.bilingual, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        return

    # translate_lines 严重异常时返回 None,同样整体回退为中文输出
    if translated is None:
        with open(args.en, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        with open(args.bilingual, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        return

    # 按解析顺序把翻译结果放回各分组
    idx = 0
    for _, lines in sections:
        for i in range(len(lines)):
            lines[i] = translated[idx]
            idx += 1

    with open(args.en, "w", encoding="utf-8", newline="\n") as f:
        f.write(render_en(sections))
    with open(args.bilingual, "w", encoding="utf-8", newline="\n") as f:
        f.write(render_bilingual(sections, original_lines))
    print(f"已生成 {args.en}（纯英文）和 {args.bilingual}（中英双语）")


if __name__ == "__main__":
    main()
