from __future__ import annotations

import argparse
import json
from collections import defaultdict
from datetime import date
from pathlib import Path
from typing import Any


AI_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATA_ROOT = AI_ROOT / "data"
DEFAULT_OUTPUT = DEFAULT_DATA_ROOT / "drafts" / "sejong" / "wiki-drafts-by-year.json"
DEFAULT_SQL_OUTPUT = AI_ROOT.parent / "database" / "seed-sejong-yearly-wiki-drafts.sql"
ACADEMIC_NOTICE_URL = "https://www.sejong.ac.kr/kor/intro/notice3.do"


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def markdown_table(headers: list[str], rows: list[list[object]]) -> str:
    def clean(value: object) -> str:
        return str(value or "-").replace("|", "\\|").replace("\n", " ").strip()

    lines = [
        "| " + " | ".join(headers) + " |",
        "| " + " | ".join("---" for _ in headers) + " |",
    ]
    lines.extend("| " + " | ".join(clean(value) for value in row) + " |" for row in rows)
    return "\n".join(lines)


def course_schedule_drafts(data_root: Path) -> list[dict[str, Any]]:
    data = read_json(data_root / "normalized" / "sejong" / "software-course-schedules.json")
    drafts = []
    for term_data in data["terms"]:
        term = term_data["term"]
        year = int(term.split("-")[0])
        rows = [
            [
                course.get("학수번호"),
                course.get("분반"),
                course.get("교과목명"),
                course.get("이수구분"),
                course.get("학점"),
                course.get("메인 교수명"),
                course.get("요일 및 강의시간"),
                course.get("강의실"),
            ]
            for course in term_data["courses"]
        ]
        source_url = (
            f"{ACADEMIC_NOTICE_URL}?articleNo={term_data['sourceArticleNo']}&mode=view"
            if term_data.get("sourceArticleNo")
            else ACADEMIC_NOTICE_URL
        )
        content = (
            f"# {term} 소프트웨어학과 강의시간표\n\n"
            f"{term} 학기의 소프트웨어학과 및 콘텐츠소프트웨어학과 개설 강좌 "
            f"{term_data['courseCount']}개를 정리한 초안입니다. 다른 학기의 정보는 포함하지 않았습니다.\n\n"
            + markdown_table(
                ["학수번호", "분반", "교과목명", "이수구분", "학점", "교수", "강의시간", "강의실"],
                rows,
            )
            + f"\n\n## 출처\n- 세종대학교 {term} 강의시간표: {source_url}\n"
            "- 실제 수강신청 전 학사정보시스템에서 변경 여부를 다시 확인해야 합니다."
        )
        drafts.append(
            make_draft(
                draft_id=f"course-schedule-{term}",
                year=year,
                term=term,
                category="교과목",
                title=f"{term} 소프트웨어학과 강의시간표",
                summary=f"{term} 소프트웨어학과 개설 강좌 {term_data['courseCount']}개와 교수·시간·강의실 정보",
                content=content,
                sources=[{"title": f"{term} 강의시간표", "url": source_url}],
                source_count=term_data["courseCount"],
            )
        )
    return drafts


def curriculum_drafts(data_root: Path) -> list[dict[str, Any]]:
    data = read_json(data_root / "normalized" / "sejong" / "curriculum-comparison.json")
    comparisons = {item["toYear"]: item for item in data.get("comparisons", [])}
    drafts = []
    for year_data in data["years"]:
        year = int(year_data["year"])
        rows = [
            [
                course.get("학년"),
                course.get("교과목명"),
                course.get("이수구분"),
                course.get("학점/이론/실습"),
            ]
            for course in year_data["courses"]
        ]
        comparison = comparisons.get(year)
        change_section = ""
        if comparison:
            def change_label(item: object) -> str:
                if isinstance(item, dict):
                    return f"{item.get('courseName', '-')} ({item.get('completionType', '-')}, {item.get('credits', '-')})"
                return str(item)

            added = ", ".join(change_label(item) for item in comparison.get("added", [])) or "없음"
            removed = ", ".join(change_label(item) for item in comparison.get("removed", [])) or "없음"
            change_section = (
                f"\n\n## {comparison['fromYear']}년 대비 변경\n"
                f"- 추가: {added}\n"
                f"- 제외: {removed}"
            )
        content = (
            f"# {year}년 소프트웨어학과 교과과정\n\n"
            f"{year}년 교과과정 {year_data['courseCount']}개를 정리한 초안입니다. "
            "교육과정 연도와 실제 수강 학기는 다를 수 있으므로 본인의 입학년도 기준 교과과정을 확인해야 합니다.\n\n"
            + markdown_table(["학년", "교과목명", "이수구분", "학점/이론/실습"], rows)
            + change_section
            + "\n\n## 출처\n- 세종대학교 학사정보에서 내려받은 해당 연도 교과과정 목록"
        )
        drafts.append(
            make_draft(
                draft_id=f"software-curriculum-{year}",
                year=year,
                term=None,
                category="교과목",
                title=f"{year}년 소프트웨어학과 교과과정",
                summary=f"{year}년 적용 교과과정 {year_data['courseCount']}개와 전년도 대비 변경 사항",
                content=content,
                sources=[{"title": f"{year}년 교과과정 목록", "url": "학사정보시스템 내려받기 자료"}],
                source_count=year_data["courseCount"],
            )
        )
    return drafts


def notice_drafts(data_root: Path) -> list[dict[str, Any]]:
    raw_root = data_root / "raw" / "sejong"
    channels = [
        ("department", "학과 공지", "학교생활", raw_root / "department-notices" / "all-notices-since-2024.json"),
        ("scholarship", "장학 공지", "장학·지원", raw_root / "student-support" / "scholarship-notices-since-2024.json"),
        ("career", "진로·취업 공지", "진로·취업", raw_root / "student-support" / "career-notices-since-2024.json"),
        ("field-practice", "현장실습 공지", "현장실습", raw_root / "student-support" / "field-practice-notices-since-2024.json"),
        ("sw-program", "SW중심대학사업단 공지", "프로젝트", raw_root / "sw-programs" / "notices.json"),
        ("tosc", "TOSC 공지", "인증제도", raw_root / "tosc" / "notices-since-2024.json"),
    ]
    drafts = []
    for slug, label, category, path in channels:
        grouped: dict[int, list[dict[str, Any]]] = defaultdict(list)
        for item in read_json(path):
            published = str(item.get("published", ""))
            if len(published) >= 4 and published[:4].isdigit():
                grouped[int(published[:4])].append(item)
        for year, items in sorted(grouped.items()):
            items.sort(key=lambda item: (str(item.get("published", "")), int(item.get("articleNo", item.get("id", 0)))), reverse=True)
            rows = [[item.get("published"), item.get("title"), item.get("url")] for item in items]
            content = (
                f"# {year}년 {label}\n\n"
                f"{year}년에 게시된 {label} {len(items)}건의 색인입니다. "
                "다른 연도의 공지는 포함하지 않았으며, 신청 기한과 자격은 반드시 원문에서 확인해야 합니다.\n\n"
                + markdown_table(["게시일", "제목", "원문"], rows)
                + f"\n\n## 출처\n- 각 항목의 공식 게시판 원문 링크\n- 집계 연도: {year}년"
            )
            drafts.append(
                make_draft(
                    draft_id=f"{slug}-{year}",
                    year=year,
                    term=None,
                    category=category,
                    title=f"{year}년 {label} 모음",
                    summary=f"{year}년에 게시된 {label} {len(items)}건의 날짜별 공식 원문 색인",
                    content=content,
                    sources=[
                        {"title": item["title"], "url": item["url"], "published": item["published"]}
                        for item in items
                    ],
                    source_count=len(items),
                )
            )
    return drafts


def make_draft(**values: Any) -> dict[str, Any]:
    return {"status": "DRAFT", **values}


def sql_string(value: object) -> str:
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def render_sql(drafts: list[dict[str, Any]]) -> str:
    categories = sorted({draft["category"] for draft in drafts})
    lines = [
        "USE uniwiki_ai;",
        "",
        "INSERT INTO users (email, password, nickname, role)",
        "VALUES ('official-source@local.invalid', SHA2(UUID(), 256), '세종대 공식자료', 'USER')",
        "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);",
        "",
    ]
    for category in categories:
        lines.extend(
            [
                "INSERT INTO categories (name, description)",
                f"SELECT {sql_string(category)}, {sql_string(category + ' 연도별 공식자료 초안')}",
                f"WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = {sql_string(category)});",
            ]
        )
    lines.extend(["", "SET @source_author_id = (SELECT id FROM users WHERE email = 'official-source@local.invalid');", ""])
    for draft in drafts:
        content = draft["content"] + f"\n\n초안 ID: {draft['draft_id']}"
        lines.extend(
            [
                "SET @category_id = (SELECT id FROM categories WHERE name = " + sql_string(draft["category"]) + ");",
                "INSERT INTO wiki_posts (category_id, author_id, title, content, summary, status)",
                "SELECT @category_id, @source_author_id, "
                f"       {sql_string(draft['title'])}, {sql_string(content)}, {sql_string(draft['summary'])}, 'DRAFT'",
                f"WHERE NOT EXISTS (SELECT 1 FROM wiki_posts WHERE title = {sql_string(draft['title'])});",
                "",
            ]
        )
    return "\n".join(lines)


def build_drafts(data_root: Path) -> list[dict[str, Any]]:
    drafts = course_schedule_drafts(data_root) + curriculum_drafts(data_root) + notice_drafts(data_root)
    return sorted(drafts, key=lambda item: (item["year"], item.get("term") or "", item["category"], item["title"]))


def main() -> int:
    parser = argparse.ArgumentParser(description="Create year-separated Sejong wiki drafts")
    parser.add_argument("--data-root", type=Path, default=DEFAULT_DATA_ROOT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--sql-output", type=Path, default=DEFAULT_SQL_OUTPUT)
    arguments = parser.parse_args()

    drafts = build_drafts(arguments.data_root)
    payload = {
        "generatedAt": date.today().isoformat(),
        "rule": "Each draft contains one calendar year, or one semester for course schedules.",
        "draftCount": len(drafts),
        "drafts": drafts,
    }
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    arguments.sql_output.write_text(render_sql(drafts) + "\n", encoding="utf-8")

    counts: dict[int, int] = defaultdict(int)
    for draft in drafts:
        counts[draft["year"]] += 1
    print(f"created {len(drafts)} drafts: " + ", ".join(f"{year}={count}" for year, count in sorted(counts.items())))
    print(f"json: {arguments.output}")
    print(f"sql: {arguments.sql_output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
