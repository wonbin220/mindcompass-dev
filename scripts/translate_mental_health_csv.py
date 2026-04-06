# 한국어 멘탈헬스 CSV 번역 배치와 체크포인트 저장을 처리하는 스크립트다.
from __future__ import annotations

import argparse
import csv
import json
import os
import re
import socket
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


API_URL = "https://api.openai.com/v1/chat/completions"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="OpenAI를 사용해 멘탈헬스 CSV의 텍스트 컬럼을 한국어로 번역한다."
    )
    parser.add_argument("--input", required=True, help="원본 CSV 경로")
    parser.add_argument("--output", required=True, help="번역 결과 CSV 경로")
    parser.add_argument(
        "--text-columns",
        required=True,
        help="번역할 텍스트 컬럼명. 쉼표로 구분한다. 예: text 또는 Context,Response,LLM",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=20,
        help="한 번에 번역할 행 수. 긴 상담 데이터는 5~10 권장",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=0,
        help="0이면 전체, 양수면 앞에서부터 해당 개수만 처리",
    )
    parser.add_argument(
        "--model",
        default="gpt-4o-mini",
        help="사용할 OpenAI 모델명",
    )
    parser.add_argument(
        "--temperature",
        type=float,
        default=0.2,
        help="번역 안정성을 위한 낮은 온도 권장",
    )
    parser.add_argument(
        "--suffix",
        default="_ko",
        help="번역 컬럼 suffix. 기본값은 _ko",
    )
    parser.add_argument(
        "--sleep-seconds",
        type=float,
        default=0.5,
        help="배치 사이 대기 시간",
    )
    parser.add_argument(
        "--request-timeout",
        type=float,
        default=300,
        help="OpenAI 요청 타임아웃 초",
    )
    parser.add_argument(
        "--max-retries",
        type=int,
        default=4,
        help="OpenAI 요청 재시도 횟수",
    )
    parser.add_argument(
        "--resume-mode",
        choices=["contiguous", "missing"],
        default="contiguous",
        help="contiguous는 처음 비어 있는 행부터 이어서 처리하고, missing은 비어 있는 행만 다시 처리한다.",
    )
    return parser.parse_args()


def read_csv_rows(csv_path: Path) -> tuple[list[dict[str, str]], list[str]]:
    encodings = ["utf-8-sig", "utf-8", "cp1252", "latin-1"]
    last_error: Exception | None = None
    for encoding in encodings:
        try:
            with csv_path.open("r", encoding=encoding, newline="") as handle:
                reader = csv.DictReader(handle)
                rows = list(reader)
                if reader.fieldnames is None:
                    raise ValueError(f"CSV 헤더를 읽을 수 없습니다: {csv_path}")
                return rows, list(reader.fieldnames)
        except Exception as exc:  # noqa: BLE001
            last_error = exc
    raise RuntimeError(f"CSV 읽기 실패: {csv_path}\n원인: {last_error}") from last_error


def normalize_text(value: Any) -> str:
    if value is None:
        return ""
    text = str(value)
    return text.encode("utf-8", errors="replace").decode("utf-8")


def is_effectively_empty_source(text: str) -> bool:
    normalized = normalize_text(text).strip()
    if not normalized:
        return True

    lowered = normalized.lower()
    if lowered.startswith("<!--[if gte mso") or "<w:lsdexception" in lowered:
        return True

    return False


def sanitize_for_json(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: sanitize_for_json(item) for key, item in value.items()}
    if isinstance(value, list):
        return [sanitize_for_json(item) for item in value]
    if isinstance(value, str):
        return normalize_text(value)
    return value


def choose_copula(text: str, replacement_for_vowel: str = "예요", replacement_for_final: str = "이에요") -> str:
    stripped = text.rstrip()
    if not stripped:
        return replacement_for_final
    last_char = stripped[-1]
    if not ("가" <= last_char <= "힣"):
        return replacement_for_final
    has_final_consonant = (ord(last_char) - ord("가")) % 28 != 0
    return replacement_for_final if has_final_consonant else replacement_for_vowel


def apply_phrase_replacements(text: str) -> str:
    replacements = [
        ("안타깝습니다", "안타까워요"),
        ("기쁩니다", "기뻐요"),
        ("중요합니다", "중요해요"),
        ("필요합니다", "필요해요"),
        ("가능합니다", "가능해요"),
        ("권장합니다", "권해요"),
        ("추천합니다", "추천해요"),
        ("조언드립니다", "조언드려요"),
        ("도움이 됩니다", "도움이 돼요"),
        ("도움이 될 수 있습니다", "도움이 될 수 있어요"),
        ("될 수 있습니다", "될 수 있어요"),
        ("할 수 있습니다", "할 수 있어요"),
        ("않습니다", "않아요"),
        ("있습니다", "있어요"),
        ("없습니다", "없어요"),
        ("같습니다", "같아요"),
        ("싶습니다", "싶어요"),
        ("좋습니다", "좋아요"),
        ("괜찮습니다", "괜찮아요"),
        ("어렵습니다", "어려워요"),
        ("쉽습니다", "쉬워요"),
        ("바랍니다", "바라요"),
        ("드립니다", "드려요"),
        ("해드립니다", "해드려요"),
        ("부탁드립니다", "부탁드려요"),
        ("감사드립니다", "감사드려요"),
        ("죄송합니다", "죄송해요"),
        ("축하합니다", "축하해요"),
        ("이해합니다", "이해해요"),
        ("생각합니다", "생각해요"),
        ("설명합니다", "설명해요"),
        ("의미합니다", "의미해요"),
        ("말씀드립니다", "말씀드려요"),
    ]
    normalized = text
    for source, target in replacements:
        normalized = normalized.replace(source, target)
    return normalized


def normalize_sentence_endings(text: str) -> str:
    normalized = text
    normalized = re.sub(r"입니다([.!?]|$)", lambda m: choose_copula(normalized[: m.start()]) + m.group(1), normalized)
    normalized = re.sub(r"입니다,", lambda m: choose_copula(normalized[: m.start()]) + ",", normalized)
    normalized = re.sub(r"합니다([.!?]|$)", r"해요\1", normalized)
    normalized = re.sub(r"합니다,", "해요,", normalized)
    normalized = re.sub(r"됩니다([.!?]|$)", r"돼요\1", normalized)
    normalized = re.sub(r"됩니다,", "돼요,", normalized)
    normalized = re.sub(r"였습니다([.!?]|$)", r"이었어요\1", normalized)
    normalized = re.sub(r"였습니다,", "이었어요,", normalized)
    normalized = re.sub(r"됩니다", "돼요", normalized)
    normalized = re.sub(r"합니다", "해요", normalized)
    normalized = re.sub(r"있습니다", "있어요", normalized)
    normalized = re.sub(r"없습니다", "없어요", normalized)
    return normalized


def normalize_to_haeyo_style(text: str) -> str:
    normalized = normalize_text(text)
    normalized = apply_phrase_replacements(normalized)
    normalized = normalize_sentence_endings(normalized)
    normalized = normalized.replace("저는", "저는")
    return normalized


def build_messages(batch: list[dict[str, object]], text_columns: list[str]) -> list[dict[str, str]]:
    system_prompt = (
        "You translate English mental-health dataset text into natural Korean for a Korean-language service.\n"
        "Return JSON only.\n"
        "Rules:\n"
        "- Preserve the original meaning, risk level, and emotional nuance.\n"
        "- Do not add advice, interpretation, or safety warnings that are not in the source.\n"
        "- Use plain natural Korean, not literal word-for-word translation.\n"
        "- Prefer fluent Korean phrasing over rigid sentence-by-sentence mirroring.\n"
        "- For short emotion statements, avoid stiff translations like direct dictionary matches.\n"
        "- Translate first-person feeling expressions in a way that sounds natural in Korean.\n"
        "- Use one fixed Korean voice across all translated fields in this dataset run.\n"
        "- Default to warm natural 해요체 for self-report text and counseling/supportive response text.\n"
        "- Keep the Korean speech level consistent within each translated field.\n"
        "- Do not mix styles such as 해, 해요, and 합니다 in one translated field.\n"
        "- Keep sentence endings consistently in 해요체 unless the source contains a quoted phrase.\n"
        "- Avoid switching between 너, 당신, and other second-person references.\n"
        "- Prefer omitting the second-person subject when natural, and use 당신 only when clearly needed.\n"
        "- Keep first-person self-report text natural while still using 해요체.\n"
        "- If a literal noun choice sounds unnatural in Korean, choose the more natural wording as long as the mental-health meaning stays the same.\n"
        "- Keep relationship, self-worth, anxiety, depression, and crisis wording precise and cautious.\n"
        "- Keep line breaks when they matter.\n"
        "- Keep empty strings empty.\n"
        "- Return an object with a single key named translations.\n"
        "- translations must be an array of objects.\n"
        "- Each object must contain id plus translated values for every requested text column.\n"
    )

    user_prompt = {
        "columns": text_columns,
        "rows": batch,
    }

    return [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": json.dumps(sanitize_for_json(user_prompt), ensure_ascii=False)},
    ]


def call_openai(
    api_key: str,
    model: str,
    temperature: float,
    batch: list[dict[str, object]],
    text_columns: list[str],
    request_timeout: float,
    max_retries: int,
) -> list[dict[str, str]]:
    payload = {
        "model": model,
        "temperature": temperature,
        "response_format": {"type": "json_object"},
        "messages": build_messages(batch, text_columns),
    }
    request = urllib.request.Request(
        API_URL,
        data=json.dumps(sanitize_for_json(payload), ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    last_error: Exception | None = None
    for attempt in range(1, max_retries + 1):
        try:
            with urllib.request.urlopen(request, timeout=request_timeout) as response:
                raw = response.read().decode("utf-8")
            parsed = json.loads(raw)
            message_content = parsed["choices"][0]["message"]["content"]
            content_json = json.loads(message_content)
            translations = content_json.get("translations")
            if not isinstance(translations, list):
                raise RuntimeError(f"예상한 translations 배열이 없습니다: {message_content}")
            return translations
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"OpenAI 요청 실패: {exc.code}\n{detail}") from exc
        except (urllib.error.URLError, socket.timeout, json.JSONDecodeError, RuntimeError) as exc:
            last_error = exc
            if attempt >= max_retries:
                raise RuntimeError(f"OpenAI 응답 처리 실패: {exc}") from exc
            wait_seconds = min(2 ** (attempt - 1), 10)
            print(
                f"OpenAI 요청/응답 재시도 {attempt}/{max_retries} 실패. {wait_seconds}초 후 다시 시도합니다.",
                file=sys.stderr,
            )
            time.sleep(wait_seconds)
    else:
        raise RuntimeError(f"OpenAI 응답 처리 실패: {last_error}")


def looks_like_garbled_text(text: str) -> bool:
    normalized = normalize_text(text).strip()
    if not normalized:
        return False

    mojibake_markers = ["ð", "â€", "Ã", "ƒ", "??", "Ÿ"]
    if sum(normalized.count(marker) for marker in mojibake_markers) >= 3:
        return True

    allowed_chars = sum(char.isalnum() or char.isspace() for char in normalized)
    return allowed_chars / max(len(normalized), 1) < 0.45


def translate_batch_with_fallback(
    api_key: str,
    model: str,
    temperature: float,
    request_rows: list[dict[str, object]],
    text_columns: list[str],
    request_timeout: float,
    max_retries: int,
) -> list[dict[str, str]]:
    try:
        return call_openai(
            api_key=api_key,
            model=model,
            temperature=temperature,
            batch=request_rows,
            text_columns=text_columns,
            request_timeout=request_timeout,
            max_retries=max_retries,
        )
    except RuntimeError:
        if len(request_rows) == 1:
            request_row = request_rows[0]
            if all(looks_like_garbled_text(str(request_row.get(column, ""))) for column in text_columns):
                print(
                    f"Garbled text fallback applied. id={request_row.get('id')}",
                    file=sys.stderr,
                )
                return [
                    {
                        "id": str(request_row["id"]),
                        **{column: normalize_text(request_row.get(column, "")) for column in text_columns},
                    }
                ]
            raise

        print(
            f"Batch request failed, splitting {len(request_rows)} rows into smaller retries.",
            file=sys.stderr,
        )
        midpoint = max(1, len(request_rows) // 2)
        collected: list[dict[str, str]] = []
        for partition in (request_rows[:midpoint], request_rows[midpoint:]):
            if not partition:
                continue
            single_result = translate_batch_with_fallback(
                api_key=api_key,
                model=model,
                temperature=temperature,
                request_rows=partition,
                text_columns=text_columns,
                request_timeout=request_timeout,
                max_retries=max_retries,
            )
            collected.extend(single_result)
        return collected


def merge_translations(
    rows: list[dict[str, str]],
    translations: list[dict[str, str]],
    text_columns: list[str],
    suffix: str,
) -> None:
    by_id = {str(item["id"]): item for item in translations}
    for row_index, row in enumerate(rows):
        item = by_id.get(str(row_index))
        if item is None:
            continue
        for column in text_columns:
            row[f"{column}{suffix}"] = normalize_to_haeyo_style(item.get(column, ""))


def write_csv_rows(
    output_path: Path,
    rows: list[dict[str, str]],
    fieldnames: list[str],
    text_columns: list[str],
    suffix: str,
) -> None:
    translated_fields = [f"{column}{suffix}" for column in text_columns]
    output_fields = fieldnames + [field for field in translated_fields if field not in fieldnames]
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temp_handle = tempfile.NamedTemporaryFile(
        "w",
        encoding="utf-8-sig",
        newline="",
        dir=output_path.parent,
        prefix=f"{output_path.stem}.",
        suffix=".tmp",
        delete=False,
    )
    temp_path = Path(temp_handle.name)
    try:
        with temp_handle:
            writer = csv.DictWriter(temp_handle, fieldnames=output_fields)
            writer.writeheader()
            writer.writerows(rows)
            temp_handle.flush()
            os.fsync(temp_handle.fileno())
        os.replace(temp_path, output_path)
    except Exception:  # noqa: BLE001
        temp_path.unlink(missing_ok=True)
        raise


def count_completed_rows(rows: list[dict[str, str]], text_columns: list[str], suffix: str) -> int:
    completed = 0
    for row in rows:
        if all(
            is_effectively_empty_source(row.get(column, ""))
            or normalize_text(row.get(f"{column}{suffix}", "")).strip()
            for column in text_columns
        ):
            completed += 1
        else:
            break
    return completed


def find_missing_row_indexes(rows: list[dict[str, str]], text_columns: list[str], suffix: str) -> list[int]:
    missing_indexes: list[int] = []
    for row_index, row in enumerate(rows):
        if any(
            not is_effectively_empty_source(row.get(column, ""))
            and not normalize_text(row.get(f"{column}{suffix}", "")).strip()
            for column in text_columns
        ):
            missing_indexes.append(row_index)
    return missing_indexes


def load_rows_for_resume(
    input_path: Path,
    output_path: Path,
    text_columns: list[str],
    suffix: str,
) -> tuple[list[dict[str, str]], list[str], int]:
    base_rows, base_fieldnames = read_csv_rows(input_path)
    if not output_path.exists():
        return base_rows, base_fieldnames, 0
    if output_path.stat().st_size == 0:
        return base_rows, base_fieldnames, 0

    output_rows, output_fieldnames = read_csv_rows(output_path)
    if len(output_rows) != len(base_rows):
        return base_rows, base_fieldnames, 0

    for column in text_columns:
        translated_column = f"{column}{suffix}"
        if translated_column not in output_fieldnames:
            return base_rows, base_fieldnames, 0

    return output_rows, output_fieldnames, count_completed_rows(output_rows, text_columns, suffix)


def acquire_output_lock(output_path: Path) -> Path:
    lock_path = output_path.with_name(f"{output_path.name}.lock")
    try:
        fd = os.open(str(lock_path), os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except FileExistsError as exc:
        raise RuntimeError(f"Another translation process is already using this output: {lock_path}") from exc

    with os.fdopen(fd, "w", encoding="utf-8") as handle:
        handle.write(str(os.getpid()))
    return lock_path


def release_output_lock(lock_path: Path) -> None:
    lock_path.unlink(missing_ok=True)


def main() -> int:
    args = parse_args()
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        print("OPENAI_API_KEY 환경변수가 없습니다.", file=sys.stderr)
        return 1

    input_path = Path(args.input)
    output_path = Path(args.output)
    text_columns = [column.strip() for column in args.text_columns.split(",") if column.strip()]
    if not text_columns:
        print("번역할 text column이 없습니다.", file=sys.stderr)
        return 1

    rows, fieldnames, completed_rows = load_rows_for_resume(
        input_path=input_path,
        output_path=output_path,
        text_columns=text_columns,
        suffix=args.suffix,
    )
    if args.limit > 0:
        rows = rows[: args.limit]

    for column in text_columns:
        if column not in fieldnames:
            print(f"컬럼을 찾을 수 없습니다: {column}", file=sys.stderr)
            return 1
        for row in rows:
            row.setdefault(f"{column}{args.suffix}", "")

    total = len(rows)
    if completed_rows > total:
        completed_rows = 0

    if args.resume_mode == "missing":
        target_indexes = find_missing_row_indexes(rows, text_columns, args.suffix)
        if target_indexes:
            print(f"기존 출력 파일의 누락 행만 다시 처리합니다. 누락 행: {len(target_indexes)}/{total}")
    else:
        target_indexes = list(range(completed_rows, total))

    if args.resume_mode == "contiguous" and completed_rows > 0:
        print(f"기존 출력 파일을 이어서 사용합니다. 완료된 행: {completed_rows}/{total}")

    for batch_start in range(0, len(target_indexes), args.batch_size):
        batch_indexes = target_indexes[batch_start : batch_start + args.batch_size]
        request_rows: list[dict[str, object]] = []
        for row_index in batch_indexes:
            row = rows[row_index]
            request_item: dict[str, object] = {"id": row_index}
            for column in text_columns:
                request_item[column] = normalize_text(row.get(column, ""))
            request_rows.append(request_item)

        translations = translate_batch_with_fallback(
            api_key=api_key,
            model=args.model,
            temperature=args.temperature,
            request_rows=request_rows,
            text_columns=text_columns,
            request_timeout=args.request_timeout,
            max_retries=args.max_retries,
        )
        merge_translations(rows, translations, text_columns, args.suffix)
        write_csv_rows(output_path, rows, fieldnames, text_columns, args.suffix)
        print(f"[{batch_start + len(batch_indexes):>5}/{len(target_indexes)}] 번역 완료")
        if batch_start + len(batch_indexes) < len(target_indexes) and args.sleep_seconds > 0:
            time.sleep(args.sleep_seconds)

    print(f"완료: {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
