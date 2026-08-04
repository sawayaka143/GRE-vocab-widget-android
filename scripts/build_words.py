#!/usr/bin/env python3
"""Enrich app/src/main/assets/words.json with definitions and example sentences.

Reads the current word decks out of words.json, looks each unique word up in the
Free Dictionary API (https://dictionaryapi.dev), and writes the definitions and
example sentences back into words.json -- preserving deck and word order.

The per-word results are cached under scripts/.cache/, so re-running the script
only fetches the words that are new or failed on the previous run.
"""

import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORDS_JSON = os.path.join(ROOT, "app", "src", "main", "assets", "words.json")
CACHE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".cache")

API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/{word}"
SECONDS_BETWEEN_REQUESTS = 1.0
MAX_RETRIES = 4
REQUEST_TIMEOUT = 20
USER_AGENT = "gre-flashcards-build/1.0"


def cache_path(word: str) -> str:
    digest = hashlib.md5(word.encode("utf-8")).hexdigest()
    return os.path.join(CACHE_DIR, digest + ".json")


def fetch(word: str) -> tuple[int, object | None]:
    """Fetch one word. Returns (http_status, parsed_json).

    Transient failures (network errors, 5xx, 429) are retried, then reported as
    status 0 so the caller can skip the word and retry it on a later run.
    """
    url = API_URL.format(word=urllib.parse.quote(word))
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})

    for attempt in range(MAX_RETRIES + 1):
        try:
            with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return 404, None
            if error.code in (429, 500, 502, 503, 504):
                time.sleep(2 ** attempt)
                continue
            return error.code, None
        except (urllib.error.URLError, TimeoutError, OSError):
            time.sleep(2 ** attempt)

    return 0, None


def extract(body: list | dict) -> tuple[str | None, str | None]:
    """Pull the first definition and the first example sentence from an API response.

    The API returns a list of entry objects (one per sense/etymology); a word can
    have several entries, so scan all of them.
    """
    definition = None
    example = None
    entries = body if isinstance(body, list) else [body]
    for entry in entries:
        for meaning in entry.get("meanings", []):
            for item in meaning.get("definitions", []):
                if definition is None and item.get("definition"):
                    definition = item["definition"].strip()
                if example is None and item.get("example"):
                    example = item["example"].strip()
                if definition is not None and example is not None:
                    return definition, example
    return definition, example


def main() -> int:
    os.makedirs(CACHE_DIR, exist_ok=True)

    with open(WORDS_JSON, encoding="utf-8") as handle:
        data = json.load(handle)

    decks = data["decks"]
    ordered_words = [w["word"] for deck in decks for w in deck["words"]]
    unique_words = list(dict.fromkeys(ordered_words))

    fetched = missed = cached = 0
    results: dict[str, dict] = {}

    for index, word in enumerate(unique_words, start=1):
        cache = cache_path(word)
        if os.path.exists(cache):
            with open(cache, encoding="utf-8") as handle:
                results[word] = json.load(handle)
            cached += 1
            continue

        status, body = fetch(word)
        time.sleep(SECONDS_BETWEEN_REQUESTS)

        if status == 200 and body:
            definition, example = extract(body)
            record = {"word": word, "status": "ok"}
            if definition:
                record["definition"] = definition
            if example:
                record["example"] = example
            fetched += 1
        elif status == 404:
            record = {"word": word, "status": "missing"}
            missed += 1
        else:
            print(f"  ! {word}: transient failure (status {status}), will retry next run")
            continue

        with open(cache, "w", encoding="utf-8") as handle:
            json.dump(record, handle, ensure_ascii=False)
        results[word] = record

        if index % 25 == 0:
            print(f"  {index}/{len(unique_words)} words processed")

    pending = len(unique_words) - len(results)
    for deck in decks:
        for word in deck["words"]:
            record = results.get(word["word"])
            if not record:
                continue
            if record.get("definition"):
                word["definition"] = record["definition"]
            if record.get("example"):
                word["example"] = record["example"]

    with open(WORDS_JSON, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    print(f"done: {fetched} fetched, {missed} not found, {cached} from cache, {pending} pending retries")
    missing = [w for w, r in results.items() if r.get("status") == "missing"]
    if missing:
        print("no entry found:", ", ".join(missing))
    return 0


if __name__ == "__main__":
    sys.exit(main())