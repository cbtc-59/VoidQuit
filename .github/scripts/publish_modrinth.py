# -*- coding: utf-8 -*-
"""
VoidQuit → Modrinth 发布脚本（CI 与手动通用）

版本号从 gradle.properties 的 mod_version 读取（单一真源），
变更日志从 CHANGELOG_PATH 环境变量指定的文件读取（CI 里用 RELEASE-NOTES.md）。

用法（令牌经环境变量传入，脚本不接触令牌字面量）：
    MODRINTH_TOKEN=... CHANGELOG_PATH=RELEASE-NOTES.md python .github/scripts/publish_modrinth.py
干跑（不发布、不需要令牌，只打印将要创建的数据）：
    CHANGELOG_PATH=RELEASE-NOTES.md python .github/scripts/publish_modrinth.py --dry
"""
import ipaddress
import json
import os
import hashlib
import re
import socket
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
LIBS = ROOT / "build" / "libs"
VERSIONS_DIR = ROOT / "versions"

PROJECT_ID = "trLzuSqf"          # voidquit 在 Modrinth 的项目 id
FABRIC_API_PID = "P7dR8mSH"      # fabric-api 在 Modrinth 的项目 id（固定前置）
API = "https://api.modrinth.com/v3"
ALLOWED_PREFIX = "https://api.modrinth.com/"
UA = "voidquit-release (github.com/cbtc-59/VoidQuit)"

DRY = "--dry" in sys.argv


def mod_version():
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    m = re.search(r"mod_version\s*=\s*(\S+)", text)
    if not m:
        raise RuntimeError("gradle.properties 里找不到 mod_version")
    return m.group(1)


VERSION_NUMBER = mod_version()

CHANGELOG_PATH = Path(os.environ.get("CHANGELOG_PATH", "RELEASE-NOTES.md"))
if not CHANGELOG_PATH.is_absolute():
    CHANGELOG_PATH = ROOT / CHANGELOG_PATH
if not CHANGELOG_PATH.exists():
    print(f"错误：变更日志不存在 {CHANGELOG_PATH}")
    sys.exit(1)
CHANGELOG = CHANGELOG_PATH.read_text(encoding="utf-8")

TOKEN = os.environ.get("MODRINTH_TOKEN", "").strip()
if not DRY and not TOKEN:
    print("错误：环境变量 MODRINTH_TOKEN 为空。")
    sys.exit(1)


def expected_count():
    """期望 jar 数 = settings.json 里的节点数（单一真源）"""
    settings = json.loads((ROOT / "settings.json").read_text(encoding="utf-8"))
    return len(settings["versions"])


def safe_url(path):
    """SSRF 加固：仅允许 https + api.modrinth.com；域名解析落在私网/环回/链路本地地址则拒绝"""
    url = API + path
    if not url.startswith(ALLOWED_PREFIX):
        raise RuntimeError(f"URL 不在白名单内: {url}")
    host = urllib.parse.urlparse(url).hostname
    for info in socket.getaddrinfo(host, 443):
        ip = ipaddress.ip_address(info[4][0])
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast:
            raise RuntimeError(f"域名解析到受限地址，拒绝请求: {ip}")
    return url


class NoRedirect(urllib.request.HTTPRedirectHandler):
    """SSRF 加固：禁止跟随重定向（Modrinth API 无需重定向）"""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


OPENER = urllib.request.build_opener(NoRedirect)


def parse_jar(name):
    """voidquit-fabric-mc1.21.1-v1.0.2-2609201200.jar / -dev.jar -> (loader, mc_version)"""
    m = re.match(rf"voidquit-(fabric|neoforge)-mc(.+)-v{re.escape(VERSION_NUMBER)}-(?:dev|\d+)\.jar$", name)
    if not m:
        return None
    return m.group(1), m.group(2)


def game_versions_of(node):
    """读 versions/<节点>/gradle.properties 的 game_versions 字段（字面 \\n 分隔）"""
    props = VERSIONS_DIR / node / "gradle.properties"
    for line in props.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("game_versions"):
            return [s for s in line.split("=", 1)[1].strip().split("\\n") if s]
    raise RuntimeError(f"未找到 game_versions: {props}")


def api_get(path):
    headers = {"User-Agent": UA}
    if TOKEN:
        headers["Authorization"] = TOKEN
    req = urllib.request.Request(safe_url(path), headers=headers)
    with OPENER.open(req, timeout=30) as r:
        return json.load(r)


def api_create_version(data, jar_path):
    # v3 规格：data.file_parts 列出 multipart 中文件部分的 name，文件部分 name 与之对应
    data = dict(data, file_parts=["file"])
    boundary = "----VoidQuit" + uuid.uuid4().hex
    body = b""
    body += (
        f'--{boundary}\r\n'
        f'Content-Disposition: form-data; name="data"\r\n\r\n'
        f'{json.dumps(data)}\r\n'
    ).encode("utf-8")
    body += (
        f'--{boundary}\r\n'
        f'Content-Disposition: form-data; name="file"; filename="{jar_path.name}"\r\n\r\n'
    ).encode("utf-8")
    body += jar_path.read_bytes()
    body += f"\r\n--{boundary}--\r\n".encode("utf-8")

    req = urllib.request.Request(
        safe_url("/version"),
        data=body,
        method="POST",
        headers={
            "Authorization": TOKEN,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "User-Agent": UA,
        },
    )
    with OPENER.open(req, timeout=120) as r:
        return json.load(r)


def main():
    jars = sorted(p for p in LIBS.glob("*.jar"))
    entries = []
    for p in jars:
        parsed = parse_jar(p.name)
        if not parsed:
            continue
        loader, mcver = parsed
        node = mcver + ("-neoforge" if loader == "neoforge" else "")
        entries.append({
            "jar": p,
            "loader": loader,
            "game_versions": game_versions_of(node),
        })
    want = expected_count()
    if len(entries) != want:
        print(f"错误：期望 {want} 个 jar（settings.json 节点数），实际解析出 {len(entries)} 个")
        sys.exit(1)

    print(f"已解析 {len(entries)} 个 jar，目标项目 {PROJECT_ID}，版本 {VERSION_NUMBER}")

    if DRY:
        for e in entries:
            sha = hashlib.sha256(e["jar"].read_bytes()).hexdigest()
            print(f'- [{e["loader"]}] {e["jar"].name} -> game_versions={e["game_versions"]} sha256={sha}')
        print(f"changelog 共 {len(CHANGELOG)} 字符（{CHANGELOG_PATH.name}）")
        print("干跑结束，未发布。")
        return

    # 防重复：检查已存在的同版本条目（loader + game_versions 相同则跳过）
    existing = api_get(f"/project/{PROJECT_ID}/version")
    done_keys = set()
    for v in existing:
        if v["version_number"] == VERSION_NUMBER:
            done_keys.add((tuple(v["loaders"]), tuple(sorted(v["game_versions"]))))

    created, skipped = [], []
    for e in entries:
        key = ((e["loader"],), tuple(sorted(e["game_versions"])))
        if key in done_keys:
            skipped.append(e["jar"].name)
            continue
        data = {
            "project_id": PROJECT_ID,
            "version_number": VERSION_NUMBER,
            "version_title": "VoidQuit " + VERSION_NUMBER,
            "version_type": "release",
            "featured": True,
            "environment": "client_only",
            "game_versions": e["game_versions"],
            "loaders": [e["loader"]],
            "dependencies": (
                [{"project_id": FABRIC_API_PID, "dependency_type": "required"}]
                if e["loader"] == "fabric" else []
            ),
            "changelog": CHANGELOG,
        }
        try:
            result = api_create_version(data, e["jar"])
            created.append(e["jar"].name)
            print(f'  已创建 [{e["loader"]}] {e["game_versions"]} -> id={result.get("id")}')
        except urllib.error.HTTPError as ex:
            print(f'  失败 [{e["loader"]}] {e["game_versions"]} HTTP {ex.code}: {ex.read().decode("utf-8", "replace")[:300]}')
            sys.exit(2)
        time.sleep(0.5)

    # 校验
    final = api_get(f"/project/{PROJECT_ID}/version")
    mine = [v for v in final if v["version_number"] == VERSION_NUMBER]
    print(f"\n完成：本次新建 {len(created)} 条，跳过（已存在）{len(skipped)} 条")
    print(f"校验：Modrinth 上 {VERSION_NUMBER} 条目共 {len(mine)} 条（期望 {want}）")
    if len(mine) != want:
        print("警告：条目数不符，请到 Modrinth 页面核对！")
        sys.exit(3)
    print("全部通过。")


if __name__ == "__main__":
    main()
