import importlib.util
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "create_yearly_wiki_drafts.py"
SPEC = importlib.util.spec_from_file_location("create_yearly_wiki_drafts", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def test_drafts_are_separated_by_year_and_term():
    data_root = Path(__file__).resolve().parents[1] / "data"
    drafts = MODULE.build_drafts(data_root)

    assert drafts
    assert all(draft["status"] == "DRAFT" for draft in drafts)
    assert all(str(draft["year"]) in draft["title"] for draft in drafts)
    assert all(not draft["term"] or draft["term"].startswith(str(draft["year"])) for draft in drafts)
    assert len({draft["draft_id"] for draft in drafts}) == len(drafts)
    assert [draft["year"] for draft in drafts] == sorted(
        (draft["year"] for draft in drafts), reverse=True
    )
    for draft in drafts:
        dated_sources = [source for source in draft["sources"] if source.get("published")]
        assert all(source["published"].startswith(str(draft["year"])) for source in dated_sources)


def test_schedule_drafts_cover_each_term_once():
    data_root = Path(__file__).resolve().parents[1] / "data"
    drafts = MODULE.course_schedule_drafts(data_root)

    assert [draft["term"] for draft in drafts] == [
        "2024-1", "2024-2", "2025-1", "2025-2", "2026-1", "2026-2"
    ]
    assert all(draft["source_count"] > 0 for draft in drafts)
