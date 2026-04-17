import json
import os
import re
import hmac
import io
import hashlib
from collections import OrderedDict

from flask import Flask, jsonify, render_template, request
from flask_cors import CORS
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from dotenv import load_dotenv
from PIL import Image
import numpy as np
from paddleocr import PaddleOCR

load_dotenv()

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 16 * 1024 * 1024  # 16 MB max upload
app.config["API_KEY"] = os.getenv("OCR_API_KEY", "")


def parse_allowed_origins() -> list[str]:
    raw = os.getenv(
        "OCR_ALLOWED_ORIGINS",
        "http://localhost:8081,http://127.0.0.1:8081,http://localhost:8080,http://127.0.0.1:8080",
    ).strip()

    if not raw:
        return []

    if raw.startswith("["):
        try:
            parsed = json.loads(raw)
            if isinstance(parsed, list):
                return [str(origin).strip() for origin in parsed if str(origin).strip()]
        except json.JSONDecodeError:
            pass

    return [origin.strip() for origin in raw.split(",") if origin.strip()]


ALLOWED_ORIGINS = parse_allowed_origins()

CORS(
    app,
    resources={r"/api/*": {"origins": ALLOWED_ORIGINS}},
)

limiter = Limiter(
    key_func=get_remote_address,
    app=app,
    default_limits=[],
    storage_uri="memory://",
)

ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "webp", "bmp", "tiff", "gif"}
ocr = PaddleOCR(use_angle_cls=True, lang="en")
MAX_IMAGE_WIDTH = int(os.getenv("OCR_MAX_IMAGE_WIDTH", "1800"))
OCR_CACHE_MAX_ITEMS = int(os.getenv("OCR_CACHE_MAX_ITEMS", "128"))
ocr_result_cache: OrderedDict[str, list[dict[str, str]]] = OrderedDict()


def is_allowed_file(filename: str) -> bool:
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS


def extract_text_with_paddle(image: Image.Image) -> str:
    image_array = np.array(image)
    result = ocr.ocr(image_array, cls=True)

    lines = []
    for page in result or []:
        for item in page or []:
            if len(item) < 2:
                continue
            text_info = item[1]
            if isinstance(text_info, (list, tuple)) and text_info:
                text = text_info[0]
                if isinstance(text, str) and text.strip():
                    lines.append(text.strip())

    return "\n".join(lines)


def preprocess_image(image: Image.Image) -> Image.Image:
    # Keep OCR quality while limiting extremely large images that slow inference.
    if image.mode not in ("RGB", "L"):
        image = image.convert("RGB")

    width, height = image.size
    if width > MAX_IMAGE_WIDTH:
        scale = MAX_IMAGE_WIDTH / float(width)
        new_size = (MAX_IMAGE_WIDTH, max(1, int(height * scale)))
        image = image.resize(new_size, Image.Resampling.LANCZOS)

    return image


def get_cached_result(cache_key: str) -> list[dict[str, str]] | None:
    cached = ocr_result_cache.get(cache_key)
    if cached is None:
        return None
    # Move key to end for LRU behavior.
    ocr_result_cache.move_to_end(cache_key)
    return cached


def set_cached_result(cache_key: str, parsed_courses: list[dict[str, str]]) -> None:
    ocr_result_cache[cache_key] = parsed_courses
    ocr_result_cache.move_to_end(cache_key)
    while len(ocr_result_cache) > OCR_CACHE_MAX_ITEMS:
        ocr_result_cache.popitem(last=False)


def parse_course_json(ocr_text: str) -> list[dict[str, str]]:
    course_re = re.compile(r"\b([A-Z]{2,4}\d{3,4})\s*-\s*(.+)")
    slot_re = re.compile(r"\b([A-Z]\d\+T[A-Z]?\d)\b")
    date_re = re.compile(r"\b(\d{1,2}-[A-Za-z]{3}-\d{4})(?:\s+\d{2}:\d{2})?\b")

    records: list[dict[str, str]] = []
    seen_keys: set[tuple[str, str, str, str, str]] = set()
    current: dict[str, str] | None = None

    def infer_course_type(course_name: str) -> str:
        return "Lab" if "lab" in course_name.lower() else "Theory"

    def is_project_course(course_name: str) -> bool:
        return "project" in course_name.lower()

    def normalize_course_name(course_name: str) -> str:
        # Remove common OCR bleed from neighboring column values.
        course_name = re.sub(r"\(\s*semester\s*\)", "", course_name, flags=re.IGNORECASE)
        course_name = re.sub(r"\s+", " ", course_name).strip()
        return course_name

    def is_course_qualifier(text: str) -> bool:
        # Only attach real course-type qualifiers, not unrelated OCR noise.
        lowered = text.lower()
        keywords = ("theory", "lab", "project", "embedded")
        return any(keyword in lowered for keyword in keywords)

    def has_required_fields(record: dict[str, str]) -> bool:
        required_fields = ("course_code", "course_name", "slot", "registered_date")
        return all(record.get(field, "").strip() for field in required_fields)

    def finalize_record(record: dict[str, str] | None) -> dict[str, str] | None:
        if not record or not has_required_fields(record):
            return None

        record["course_name"] = normalize_course_name(record["course_name"])
        record["course_type"] = infer_course_type(record["course_name"])
        if record.get("_project_flag") == "1" or is_project_course(record["course_name"]):
            return None

        dedupe_key = (
            record["course_code"].strip().upper(),
            record["course_name"].strip().lower(),
            record["slot"].strip().upper(),
            record["registered_date"].strip(),
            record["course_type"].strip(),
        )
        if dedupe_key in seen_keys:
            return None

        seen_keys.add(dedupe_key)
        cleaned = {k: v for k, v in record.items() if not k.startswith("_")}
        return cleaned

    def collapse_similar_records(items: list[dict[str, str]]) -> list[dict[str, str]]:
        selected: dict[tuple[str, str, str, str], tuple[int, dict[str, str]]] = {}

        for item in items:
            key = (
                item["course_code"].strip().upper(),
                item["slot"].strip().upper(),
                item["registered_date"].strip(),
                item["course_type"].strip(),
            )

            name_lower = item["course_name"].lower()
            score = 0
            if "theory" in name_lower:
                score += 2
            if "lab" in name_lower:
                score += 1
            if "project" in name_lower:
                score -= 3
            if "semester" in name_lower:
                score -= 1

            existing = selected.get(key)
            if existing is None or score > existing[0]:
                selected[key] = (score, item)

        # Preserve deterministic order based on first appearance in items.
        ordered_keys = []
        seen_core = set()
        for item in items:
            core_key = (
                item["course_code"].strip().upper(),
                item["slot"].strip().upper(),
                item["registered_date"].strip(),
                item["course_type"].strip(),
            )
            if core_key not in seen_core:
                seen_core.add(core_key)
                ordered_keys.append(core_key)

        return [selected[key][1] for key in ordered_keys if key in selected]

    lines = [line.strip() for line in ocr_text.splitlines() if line.strip()]

    for line in lines:
        course_match = course_re.search(line)
        if course_match:
            finalized = finalize_record(current)
            if finalized:
                records.append(finalized)

            current = {
                "course_code": course_match.group(1),
                "course_name": normalize_course_name(course_match.group(2).strip()),
                "course_type": infer_course_type(course_match.group(2).strip()),
                "slot": "",
                "registered_date": "",
                "_project_flag": "1" if is_project_course(course_match.group(2)) else "0",
            }

            slot_match = slot_re.search(line)
            date_match = date_re.search(line)
            if slot_match:
                current["slot"] = slot_match.group(1)
            if date_match:
                current["registered_date"] = date_match.group(1)
            continue

        if not current:
            continue

        if line.startswith("(") and line.endswith(")") and is_course_qualifier(line):
            current["course_name"] = normalize_course_name(f"{current['course_name']} {line}".strip())
            current["course_type"] = infer_course_type(current["course_name"])
            if "project" in line.lower():
                current["_project_flag"] = "1"

        if not current["slot"]:
            slot_match = slot_re.search(line)
            if slot_match:
                current["slot"] = slot_match.group(1)

        if not current["registered_date"]:
            date_match = date_re.search(line)
            if date_match:
                current["registered_date"] = date_match.group(1)

    finalized = finalize_record(current)
    if finalized:
        records.append(finalized)

    return collapse_similar_records(records)


def process_uploaded_image(uploaded_file):
    if uploaded_file is None or uploaded_file.filename == "":
        return None, "Please provide an image file in form field 'image'.", 400

    if not is_allowed_file(uploaded_file.filename):
        return None, "Unsupported file type. Please upload an image.", 400

    try:
        image_bytes = uploaded_file.read()
        cache_key = hashlib.sha256(image_bytes).hexdigest()

        cached_courses = get_cached_result(cache_key)
        if cached_courses is not None:
            return cached_courses, "", 200

        image = Image.open(io.BytesIO(image_bytes))
        image = preprocess_image(image)

        raw_text = extract_text_with_paddle(image)
        parsed_courses = parse_course_json(raw_text)
        set_cached_result(cache_key, parsed_courses)
        return parsed_courses, "", 200
    except Exception as exc:
        return None, f"Could not process image: {exc}", 500


def is_authorized(req) -> tuple[bool, str, int]:
    expected_api_key = app.config.get("API_KEY", "")
    provided_api_key = req.headers.get("X-API-Key", "")

    if not expected_api_key:
        return False, "Server API key is not configured.", 500

    if not provided_api_key:
        return False, "Missing X-API-Key header.", 401

    if not hmac.compare_digest(provided_api_key, expected_api_key):
        return False, "Invalid API key.", 401

    return True, "", 200


@app.route("/", methods=["GET", "POST"])
def index():
    extracted_json = "[]"
    error = ""

    if request.method == "POST":
        uploaded_file = request.files.get("image")
        parsed_courses, error_message, status_code = process_uploaded_image(uploaded_file)
        if status_code == 200:
            extracted_json = json.dumps(parsed_courses, indent=2)
        else:
            error = error_message

    return render_template("index.html", extracted_json=extracted_json, error=error)


@app.route("/api/extract", methods=["POST"])
@limiter.limit("20 per minute")
def extract_api():
    authorized, auth_error, auth_status = is_authorized(request)
    if not authorized:
        return jsonify({"error": auth_error}), auth_status

    uploaded_file = request.files.get("image")
    parsed_courses, error_message, status_code = process_uploaded_image(uploaded_file)

    if status_code != 200:
        return jsonify({"error": error_message}), status_code

    return jsonify({"data": parsed_courses, "count": len(parsed_courses)}), 200


@app.errorhandler(429)
def handle_rate_limit(_err):
    return jsonify({"error": "Rate limit exceeded. Try again later."}), 429


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "healthy"}), 200


@app.route("/ready", methods=["GET"])
def ready():
    if ocr is None:
        return jsonify({"ready": False}), 503
    return jsonify({"ready": True}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=False, use_reloader=False)
