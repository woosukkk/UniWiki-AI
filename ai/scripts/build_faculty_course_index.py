from __future__ import annotations

import json
from pathlib import Path


AI_ROOT = Path(__file__).resolve().parents[1]
RAW_PATH = AI_ROOT / "data" / "raw" / "sejong" / "faculty" / "software-faculty.json"
SCHEDULE_PATH = AI_ROOT / "data" / "normalized" / "sejong" / "software-course-schedules.json"
OUTPUT_PATH = AI_ROOT / "data" / "normalized" / "sejong" / "software-faculty-course-index.json"


def read_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def build_index() -> dict[str, object]:
    source = read_json(RAW_PATH)
    schedules = read_json(SCHEDULE_PATH)
    faculty = source["faculty"]
    by_name = {person["name"]: person for person in faculty}
    teaching: dict[str, list[dict[str, object]]] = {name: [] for name in by_name}

    for term_data in schedules["terms"]:
        term = term_data["term"]
        for course in term_data["courses"]:
            professor = str(course.get("메인 교수명", "")).strip()
            if professor not in teaching:
                continue
            teaching[professor].append(
                {
                    "term": term,
                    "courseCode": course.get("학수번호", ""),
                    "section": course.get("분반", ""),
                    "courseName": course.get("교과목명", ""),
                    "completionType": course.get("이수구분", ""),
                    "credits": course.get("학점", ""),
                    "schedule": course.get("요일 및 강의시간", ""),
                    "classroom": course.get("강의실", ""),
                }
            )

    profiles = []
    for person in faculty:
        courses = sorted(teaching[person["name"]], key=lambda item: (item["term"], item["courseCode"], item["section"]), reverse=True)
        terms = sorted({item["term"] for item in courses}, reverse=True)
        profiles.append({**person, "teachingTerms": terms, "teachingCourseCount": len(courses), "teachingCourses": courses})

    return {
        "generatedFrom": [str(RAW_PATH.relative_to(AI_ROOT)), str(SCHEDULE_PATH.relative_to(AI_ROOT))],
        "sourceUrl": source["sourceUrl"],
        "facultyCount": len(profiles),
        "facultyWithTeachingHistoryCount": sum(bool(item["teachingCourses"]) for item in profiles),
        "faculty": profiles,
    }


def main() -> int:
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(json.dumps(build_index(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
