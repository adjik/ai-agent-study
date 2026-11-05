#!/usr/bin/env bash
# 阶段 2 · D0：直连 DeepSeek，只看 tool_calls（不启动 8080、不执行 getOrder）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BODY_FILE="$SCRIPT_DIR/d0-tool-calls.json"

if [[ -f "$REPO_ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$REPO_ROOT/.env"
  set +a
fi

if [[ -z "${DEEPSEEK_API_KEY:-}" ]]; then
  echo "【失败】未设置 DEEPSEEK_API_KEY。" >&2
  echo "  在项目根 $REPO_ROOT 配置 .env，或：export DEEPSEEK_API_KEY=sk-..." >&2
  exit 1
fi

echo "【信息】POST https://api.deepseek.com/chat/completions （D0，不经过本仓库 Java）" >&2
RESPONSE="$(curl -sS https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
  -d @"$BODY_FILE")"

if echo "$RESPONSE" | grep -q '"error"'; then
  echo "【失败】API 返回错误，见下方 JSON：" >&2
  if command -v jq >/dev/null 2>&1; then
    echo "$RESPONSE" | jq .
  else
    echo "$RESPONSE"
  fi
  exit 1
fi

if command -v jq >/dev/null 2>&1; then
  echo "$RESPONSE" | jq .
else
  echo "$RESPONSE"
  echo "" >&2
  echo "【提示】安装 jq 后可格式化 JSON（brew install jq）" >&2
fi

echo "" >&2
PASS=true
if ! echo "$RESPONSE" | grep -q 'tool_calls'; then
  echo "【自检未过】响应里没有 tool_calls" >&2
  PASS=false
fi
if ! echo "$RESPONSE" | grep -q 'getOrder'; then
  echo "【自检未过】响应里没有 getOrder" >&2
  PASS=false
fi
if ! echo "$RESPONSE" | grep -q 'ORD-1001'; then
  echo "【自检未过】响应里没有 ORD-1001" >&2
  PASS=false
fi
if echo "$RESPONSE" | grep -q 'PAID'; then
  echo "【自检注意】响应里出现 PAID（D0 未查库，可能是模型幻觉；有 tool_calls 仍可算 D0 过关，§9 备注）" >&2
else
  echo "【自检】未发现 PAID（符合 D0：只点菜、未执行 getOrder）" >&2
fi

if [[ "$PASS" == true ]]; then
  echo "【D0 过关】finish_reason 应为 tool_calls，且含 getOrder + ORD-1001 → 可填 stage-2 §9 第一行并勾 K1–K4" >&2
  exit 0
else
  echo "【D0 未过关】对照 stage-2.md D0「常见错误」表排查" >&2
  exit 2
fi
