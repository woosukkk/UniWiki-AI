from __future__ import annotations

import json
import re
import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


AI_ROOT = Path(__file__).resolve().parents[1]
RAW_ROOT = AI_ROOT / "data" / "raw" / "sejong"
OUTPUT_ROOT = AI_ROOT / "data" / "normalized" / "sejong"
XML_NS = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL_NS = {"r": "http://schemas.openxmlformats.org/package/2006/relationships"}
OFFICE_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
DEPARTMENT_NAMES = {"소프트웨어학과", "콘텐츠소프트웨어학과"}


def column_index(reference: str) -> int:
    letters = re.match(r"[A-Z]+", reference)
    if not letters:
        return 0
    result = 0
    for character in letters.group(0):
        result = result * 26 + ord(character) - ord("A") + 1
    return result - 1


def normalize_header(value: object) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip()


def read_shared_strings(archive: zipfile.ZipFile) -> list[str]:
    try:
        root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
    except KeyError:
        return []
    return ["".join(item.itertext()) for item in root.findall("a:si", XML_NS)]


def first_worksheet_path(archive: zipfile.ZipFile) -> str:
    workbook = ET.fromstring(archive.read("xl/workbook.xml"))
    first_sheet = workbook.find("a:sheets/a:sheet", XML_NS)
    if first_sheet is None:
        raise ValueError("Workbook has no worksheets")
    relationship_id = first_sheet.attrib[f"{{{OFFICE_REL_NS}}}id"]
    relationships = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
    for relationship in relationships.findall("r:Relationship", REL_NS):
        if relationship.attrib.get("Id") == relationship_id:
            target = relationship.attrib["Target"].lstrip("/")
            return target if target.startswith("xl/") else f"xl/{target}"
    raise ValueError("Worksheet relationship is missing")


def cell_value(cell: ET.Element, shared_strings: list[str]) -> object:
    cell_type = cell.attrib.get("t")
    if cell_type == "inlineStr":
        inline = cell.find("a:is", XML_NS)
        return "" if inline is None else "".join(inline.itertext())
    value = cell.findtext("a:v", default="", namespaces=XML_NS)
    if cell_type == "s" and value:
        return shared_strings[int(value)]
    if cell_type == "b":
        return value == "1"
    if not value:
        return ""
    try:
        number = float(value)
        return int(number) if number.is_integer() else number
    except ValueError:
        return value


def read_xlsx(path: Path) -> list[dict[str, object]]:
    with zipfile.ZipFile(path) as archive:
        shared_strings = read_shared_strings(archive)
        worksheet = ET.fromstring(archive.read(first_worksheet_path(archive)))

    raw_rows: list[list[object]] = []
    for row in worksheet.findall(".//a:sheetData/a:row", XML_NS):
        cells: dict[int, object] = {}
        for cell in row.findall("a:c", XML_NS):
            cells[column_index(cell.attrib.get("r", "A1"))] = cell_value(cell, shared_strings)
        if cells:
            width = max(cells) + 1
            raw_rows.append([cells.get(index, "") for index in range(width)])

    header_index = next(
        (
            index
            for index, row in enumerate(raw_rows[:10])
            if "교과목명" in {normalize_header(value) for value in row}
        ),
        None,
    )
    if header_index is None:
        raise ValueError(f"Could not find a header row in {path}")

    headers = [normalize_header(value) for value in raw_rows[header_index]]
    records: list[dict[str, object]] = []
    for row in raw_rows[header_index + 1 :]:
        record = {
            header: row[index] if index < len(row) else ""
            for index, header in enumerate(headers)
            if header
        }
        if any(value not in ("", None) for value in record.values()):
            records.append(record)
    return records


def clean_record(record: dict[str, object], fields: list[str]) -> dict[str, object]:
    return {field: record.get(field, "") for field in fields}


def normalize_schedules() -> dict[str, object]:
    manifest_path = RAW_ROOT / "course-schedules" / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    fields = [
        "개설대학",
        "개설학과전공",
        "학수번호",
        "분반",
        "교과목명",
        "이수구분",
        "선택영역",
        "학년 (학기)",
        "학점",
        "이론",
        "실습",
        "수업 유형",
        "학점교류 수강가능",
        "요일 및 강의시간",
        "강의실",
        "메인 교수명",
        "주관학과",
        "수강대상및유의사항",
        "강좌유형",
        "사이버강좌",
        "강의언어",
    ]
    terms: list[dict[str, object]] = []
    for source in manifest["schedules"]:
        if not source["file"].endswith("-ko.xlsx"):
            continue
        path = manifest_path.parent / source["file"]
        all_courses = read_xlsx(path)
        courses = [
            clean_record(record, fields)
            for record in all_courses
            if normalize_header(record.get("개설학과전공")) in DEPARTMENT_NAMES
        ]
        terms.append(
            {
                "term": source["term"],
                "sourceFile": source["file"],
                "sourceArticleNo": source["articleNo"],
                "courseCount": len(courses),
                "courses": courses,
            }
        )
    return {
        "generatedFrom": "official Korean course schedule workbooks",
        "departmentAliases": sorted(DEPARTMENT_NAMES),
        "terms": terms,
    }


def course_identity(record: dict[str, object]) -> tuple[str, str, str]:
    return (
        normalize_header(record.get("교과목명")),
        normalize_header(record.get("이수구분")),
        normalize_header(record.get("학점/이론/실습")),
    )


def normalize_curricula() -> dict[str, object]:
    manifest_path = RAW_ROOT / "curricula" / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    years: list[dict[str, object]] = []
    course_sets: dict[int, set[tuple[str, str, str]]] = {}
    for source in sorted(manifest["curricula"], key=lambda item: item["year"]):
        year = int(source["year"])
        records = read_xlsx(manifest_path.parent / source["file"])
        courses = [
            clean_record(
                record,
                ["학년", "개설년도", "개설학기", "교과목명", "이수구분", "학점/이론/실습"],
            )
            for record in records
        ]
        course_sets[year] = {course_identity(course) for course in courses}
        years.append({"year": year, "courseCount": len(courses), "courses": courses})

    comparisons: list[dict[str, object]] = []
    ordered_years = sorted(course_sets)
    for previous, current in zip(ordered_years, ordered_years[1:]):
        added = sorted(course_sets[current] - course_sets[previous])
        removed = sorted(course_sets[previous] - course_sets[current])
        comparisons.append(
            {
                "fromYear": previous,
                "toYear": current,
                "added": [dict(zip(("courseName", "completionType", "credits"), item)) for item in added],
                "removed": [dict(zip(("courseName", "completionType", "credits"), item)) for item in removed],
            }
        )
    return {"years": years, "comparisons": comparisons}


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    write_json(OUTPUT_ROOT / "software-course-schedules.json", normalize_schedules())
    write_json(OUTPUT_ROOT / "curriculum-comparison.json", normalize_curricula())
    print(f"Wrote normalized data to {OUTPUT_ROOT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
