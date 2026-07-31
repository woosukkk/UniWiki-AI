from __future__ import annotations

import argparse
import html
import json
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path

import httpx


AI_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT_ROOT = AI_ROOT / "data" / "raw" / "sejong"
STANDARD_ROW = re.compile(
    r'<tr[^>]*>.*?href="\?mode=view&amp;articleNo=(?P<id>\d+).*?'
    r'title="(?P<title>.*?)\s*자세히 보기".*?'
    r'<span class="b-date">\s*(?P<date>\d{4}\.\d{2}\.\d{2})',
    re.DOTALL,
)
TOSC_ROW = re.compile(
    r'<a href="(?P<href>/ko/cusomter_support/notice/view/(?P<id>\d+)[^"]*)">'
    r'(?P<title>.*?)</a>.*?<span class="reg_date">.*?'
    r'data-format="Y-m-d"[^>]*>(?P<date>\d{4}-\d{2}-\d{2})</time>',
    re.DOTALL,
)


@dataclass(frozen=True)
class StandardBoard:
    name: str
    base_url: str
    output: Path
    page_size: int = 10


def fetch_text(url: str, attempts: int = 3) -> str:
    headers = {
        "User-Agent": "Mozilla/5.0 (compatible; UniWiki-AI/1.0)",
        "Accept": "text/html,application/xhtml+xml",
    }
    for attempt in range(1, attempts + 1):
        try:
            response = httpx.get(url, headers=headers, timeout=30, follow_redirects=True)
            response.raise_for_status()
            return response.text
        except httpx.HTTPError:
            if attempt == attempts:
                curl = shutil.which("curl.exe") or shutil.which("curl")
                if curl:
                    completed = subprocess.run(
                        [curl, "--fail", "--location", "--silent", "--show-error", url],
                        check=True,
                        capture_output=True,
                    )
                    return completed.stdout.decode("utf-8", errors="replace")
                raise
            time.sleep(attempt)
    raise RuntimeError("Unreachable retry state")


def clean_title(value: str) -> str:
    return html.unescape(re.sub(r"<[^>]+>", "", value)).strip()


def load_items(path: Path, id_field: str) -> dict[int, dict[str, object]]:
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8-sig"))
    return {int(item[id_field]): item for item in data}


def write_items(path: Path, items: dict[int, dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    ordered = sorted(
        items.values(),
        key=lambda item: (str(item["published"]), int(item.get("articleNo", item.get("id", 0)))),
        reverse=True,
    )
    path.write_text(json.dumps(ordered, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def collect_standard_board(
    board: StandardBoard,
    since: str,
    max_pages: int,
) -> tuple[int, int]:
    existing = load_items(board.output, "articleNo")
    known_ids = set(existing)
    added = 0
    unchanged_pages = 0

    for page in range(max_pages):
        offset = page * board.page_size
        separator = "&" if "?" in board.base_url else "?"
        url = (
            f"{board.base_url}{separator}article.offset={offset}"
            f"&articleLimit={board.page_size}&mode=list"
        )
        matches = list(STANDARD_ROW.finditer(fetch_text(url)))
        if not matches:
            break

        page_ids: set[int] = set()
        page_has_in_range_item = False
        for match in matches:
            article_id = int(match.group("id"))
            published = match.group("date").replace(".", "-")
            page_ids.add(article_id)
            if published < since:
                continue
            page_has_in_range_item = True
            if article_id not in existing:
                added += 1
            existing[article_id] = {
                "articleNo": article_id,
                "title": clean_title(match.group("title")),
                "published": published,
                "url": f"{board.base_url}?articleNo={article_id}&mode=view",
            }

        only_known = bool(page_ids) and page_ids.issubset(known_ids)
        unchanged_pages = unchanged_pages + 1 if only_known else 0
        if unchanged_pages >= 2 or (not page_has_in_range_item and page >= 1):
            break

    if added:
        write_items(board.output, existing)
    return added, len(existing)


def collect_tosc(output: Path, since: str, max_pages: int) -> tuple[int, int]:
    base_url = "https://tosc.sejong.ac.kr"
    existing = load_items(output, "id")
    known_ids = set(existing)
    added = 0
    unchanged_pages = 0

    for page in range(1, max_pages + 1):
        matches = list(TOSC_ROW.finditer(fetch_text(f"{base_url}/ko/cusomter_support/notice?p={page}")))
        if not matches:
            break
        page_ids: set[int] = set()
        page_has_in_range_item = False
        for match in matches:
            notice_id = int(match.group("id"))
            published = match.group("date")
            page_ids.add(notice_id)
            if published < since:
                continue
            page_has_in_range_item = True
            if notice_id not in existing:
                added += 1
            relative_url = re.sub(r"\?p=\d+$", "", match.group("href"))
            existing[notice_id] = {
                "id": notice_id,
                "title": clean_title(match.group("title")),
                "published": published,
                "url": f"{base_url}{relative_url}",
            }
        only_known = bool(page_ids) and page_ids.issubset(known_ids)
        unchanged_pages = unchanged_pages + 1 if only_known else 0
        if unchanged_pages >= 2 or (not page_has_in_range_item and page >= 2):
            break

    if added:
        write_items(output, existing)
    return added, len(existing)


def write_career_subsets(output_root: Path) -> None:
    support_dir = output_root / "student-support"
    source = json.loads((support_dir / "career-notices-since-2024.json").read_text(encoding="utf-8-sig"))
    filters = {
        "field-practice-notices-since-2024.json": re.compile(r"현장실습|인턴십|ICT 학점연계"),
        "career-program-notices-since-2024.json": re.compile(
            r"대학일자리|취업지원|진로|상담|직무|취업캠프"
        ),
    }
    for file_name, pattern in filters.items():
        selected = [item for item in source if pattern.search(str(item["title"]))]
        (support_dir / file_name).write_text(
            json.dumps(selected, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )


def main() -> int:
    parser = argparse.ArgumentParser(description="Incrementally collect official Sejong notice indexes")
    parser.add_argument("--since", default="2024-01-01")
    parser.add_argument("--max-pages", type=int, default=500)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT_ROOT)
    arguments = parser.parse_args()

    boards = [
        StandardBoard(
            "department",
            "https://dept.sejong.ac.kr/softwaredpt/board/notice.do",
            arguments.output_root / "department-notices" / "all-notices-since-2024.json",
        ),
        StandardBoard(
            "career",
            "https://www.sejong.ac.kr/kor/intro/notice6.do",
            arguments.output_root / "student-support" / "career-notices-since-2024.json",
        ),
        StandardBoard(
            "scholarship",
            "https://www.sejong.ac.kr/kor/intro/notice7.do",
            arguments.output_root / "student-support" / "scholarship-notices-since-2024.json",
        ),
        StandardBoard(
            "sw-programs",
            "https://sw.sejong.ac.kr/sw/notice.do",
            arguments.output_root / "sw-programs" / "notices.json",
            page_size=30,
        ),
    ]

    career_added = 0
    for board in boards:
        added, total = collect_standard_board(board, arguments.since, arguments.max_pages)
        if board.name == "career":
            career_added = added
        print(f"{board.name}: added={added} total={total}")
    added, total = collect_tosc(
        arguments.output_root / "tosc" / "notices-since-2024.json",
        arguments.since,
        arguments.max_pages,
    )
    print(f"tosc: added={added} total={total}")
    if career_added:
        write_career_subsets(arguments.output_root)
    return 0


if __name__ == "__main__":
    sys.exit(main())
