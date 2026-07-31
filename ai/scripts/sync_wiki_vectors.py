from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request


def request_json(url: str, method: str = "GET", payload: object | None = None) -> object:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        method=method,
        headers={"Content-Type": "application/json; charset=utf-8"},
    )
    with urllib.request.urlopen(request, timeout=180) as response:
        content = response.read()
    return {} if not content else json.loads(content.decode("utf-8"))


def synchronize(backend_url: str, ai_url: str) -> int:
    posts = request_json(f"{backend_url}/api/wiki-posts")
    if not isinstance(posts, list):
        raise ValueError("Backend wiki list response is not an array")

    failures: list[tuple[int, str]] = []
    for post in posts:
        wiki_post_id = int(post["id"])
        try:
            detail = request_json(f"{backend_url}/api/wiki-posts/{wiki_post_id}")
            payload = {
                "wikiPostId": detail["id"],
                "title": detail["title"],
                "content": detail["content"],
                "categoryId": detail["categoryId"],
            }
            result = request_json(
                f"{ai_url}/api/vector-store/wiki-posts",
                method="PUT",
                payload=payload,
            )
            print(
                f"SYNCED id={wiki_post_id} "
                f"chunks={result.get('storedChunkCount', '?')} title={detail['title']}"
            )
        except (KeyError, ValueError, urllib.error.URLError) as error:
            failures.append((wiki_post_id, str(error)))
            print(f"FAILED id={wiki_post_id} error={error}", file=sys.stderr)

    stats = request_json(f"{ai_url}/api/vector-store/stats")
    print(f"VECTOR_STORE collection={stats.get('collection')} chunks={stats.get('count')}")
    if failures:
        print(f"Synchronization failed for {len(failures)} wiki posts", file=sys.stderr)
        return 1
    print(f"Synchronization completed for {len(posts)} wiki posts")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Synchronize backend wiki posts to the AI vector store")
    parser.add_argument("--backend-url", default="http://localhost:8080")
    parser.add_argument("--ai-url", default="http://localhost:8000")
    arguments = parser.parse_args()
    return synchronize(arguments.backend_url.rstrip("/"), arguments.ai_url.rstrip("/"))


if __name__ == "__main__":
    sys.exit(main())
