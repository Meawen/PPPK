#!/usr/bin/env bash
set -euo pipefail

# ---------- Config ----------
BASE="${BASE:-http://localhost:8080}"
JSON='Content-Type: application/json'
OUT_DIR="${OUT_DIR:-out}"
mkdir -p "$OUT_DIR"

# Colors
ok()  { printf "\033[32m✔ %s\033[0m\n" "$*"; }
err() { printf "\033[31m✘ %s\033[0m\n" "$*" >&2; }
note(){ printf "\033[36m➜ %s\033[0m\n" "$*"; }

# Requires curl; jq is optional (pretty JSON).
has_jq=1; command -v jq >/dev/null 2>&1 || has_jq=0

# Helpers
tmp_h="$(mktemp)"; tmp_b="$(mktemp)"
cleanup(){ rm -f "$tmp_h" "$tmp_b"; }
trap cleanup EXIT

gen_oib(){ printf "%011d" "$(( (RANDOM % 10000000000) ))"; }
now_iso(){ date -u +"%Y-%m-%dT%H:%M:%SZ"; }

status(){ sed -n '1s/.* \([0-9][0-9][0-9]\).*/\1/p' "$tmp_h"; }
location_id(){
  # grabs last numeric segment from Location header, if any
  sed -n 's/Location:[[:space:]]*.*\/\([0-9][0-9]*\)[[:space:]]*$/\1/pI' "$tmp_h" | tail -n1
}

# request functions set global STATUS, BODY, LOC_ID
POST(){ curl -sS -D "$tmp_h" -o "$tmp_b" -H "$JSON" -X POST "$BASE$1" -d "$2" >/dev/null || true; }
GET(){  curl -sS -D "$tmp_h" -o "$tmp_b" "$BASE$1" >/dev/null || true; }
PUT(){  curl -sS -D "$tmp_h" -o "$tmp_b" -H "$JSON" -X PUT "$BASE$1" -d "$2" >/dev/null || true; }
DEL(){  curl -sS -D "$tmp_h" -o "$tmp_b" -X DELETE "$BASE$1" >/dev/null || true; }

show_body(){
  if (( has_jq == 1 )); then jq . <"$tmp_b" 2>/dev/null || cat "$tmp_b"; else cat "$tmp_b"; fi
}

# ---------- Run ----------
note "Health check @ $BASE/actuator/health"
GET "/actuator/health"; code="$(status)"
[[ "$code" == "200" ]] || { err "Health check failed ($code)"; exit 1; }
ok "App UP"

# --- Patients ---
OIB1="$(gen_oib)"; OIB2="$(gen_oib)"
note "Create Patient #1 (OIB=$OIB1)"
POST "/api/patients" "$(jq -nc --arg o "$OIB1" \
  '{oib:$o,firstName:"Luka",lastName:"Miholic",birthDate:"1999-02-26",sex:"M"}')"
code="$(status)"; P1="$(location_id)"
[[ "$code" == "201" && -n "$P1" ]] || { err "Create P1 failed (code=$code)"; show_body; exit 1; }
ok "Created patient P1=$P1"

note "Create Patient #2 (OIB=$OIB2)"
POST "/api/patients" "$(jq -nc --arg o "$OIB2" \
  '{oib:$o,firstName:"Ana",lastName:"Kovac",birthDate:"1995-05-12",sex:"F"}')"
code="$(status)"; P2="$(location_id)"
[[ "$code" == "201" && -n "$P2" ]] || { err "Create P2 failed (code=$code)"; show_body; exit 1; }
ok "Created patient P2=$P2"

note "Get P1"
GET "/api/patients/$P1"; code="$(status)"
[[ "$code" == "200" ]] || { err "Get P1 failed ($code)"; show_body; exit 1; }
ok "Fetched P1"; (( has_jq == 1 )) && show_body >/dev/null

note "Search patients by surname"
GET "/api/patients?surname=Miholic&size=5"; code="$(status)"
[[ "$code" == "200" ]] || { err "Search failed ($code)"; show_body; exit 1; }
ok "Search OK"

note "Get by OIB"
GET "/api/patients/by-oib/$OIB1"; code="$(status)"
[[ "$code" == "200" ]] || { err "By-OIB failed ($code)"; show_body; exit 1; }
ok "By-OIB OK"

note "Add medical history to P1"
POST "/api/patients/$P1/history" '{"diseaseName":"Hypertension","startDate":"2020-01-10"}'
code="$(status)"; [[ "$code" == "204" || "$code" == "200" ]] || { err "Add history failed ($code)"; show_body; exit 1; }
ok "History added"

note "Add prescription to P1"
POST "/api/patients/$P1/prescriptions" '{"medication":"Atorvastatin","dosage":"20mg","instructions":"Once daily"}'
code="$(status)"; [[ "$code" == "204" || "$code" == "200" ]] || { err "Add prescription failed ($code)"; show_body; exit 1; }
ok "Prescription added"

note "Update P1 (rename)"
PUT "/api/patients/$P1" '{"firstName":"Luka-Updated"}'
code="$(status)"; [[ "$code" == "200" ]] || { err "Update P1 failed ($code)"; show_body; exit 1; }
ok "P1 updated"

note "Delete P2"
DEL "/api/patients/$P2"; code="$(status)"
[[ "$code" == "204" ]] || { err "Delete P2 failed ($code)"; show_body; exit 1; }
ok "P2 deleted"

# --- Exams ---
note "List exam types"
GET "/api/exams/types"; code="$(status)"
[[ "$code" == "200" ]] || { err "List types failed ($code)"; show_body; exit 1; }
TYPE_CODE="$(jq -r '.[0].code' <"$tmp_b" 2>/dev/null || head -n1 "$tmp_b")"
[[ -n "$TYPE_CODE" && "$TYPE_CODE" != "null" ]] || { err "No exam types available"; exit 1; }
ok "Using type: $TYPE_CODE"

note "Create exam for P1"
OCCURRED="$(now_iso)"
POST "/api/exams" "$(jq -nc --argjson pid "$P1" --arg when "$OCCURRED" --arg t "$TYPE_CODE" \
  '{patientId:$pid, occurredAt:$when, examType:$t, notes:"baseline"}')"
code="$(status)"; EX1="$(location_id)"
[[ "$code" == "201" && -n "$EX1" ]] || { err "Create exam failed (code=$code)"; show_body; exit 1; }
ok "Created exam EX1=$EX1"

note "Get EX1"
GET "/api/exams/$EX1"; code="$(status)"
[[ "$code" == "200" ]] || { err "Get exam failed ($code)"; show_body; exit 1; }
ok "Fetched exam"; (( has_jq == 1 )) && show_body >/dev/null

note "List exams by patient"
GET "/api/exams?patientId=$P1&size=5"; code="$(status)"
[[ "$code" == "200" ]] || { err "List exams failed ($code)"; show_body; exit 1; }
ok "Exams listed"

note "Add attachment to EX1"
OBJ_KEY="exams/$EX1/blood_001.pdf"
POST "/api/exams/$EX1/attachments" "$(jq -nc --arg k "$OBJ_KEY" \
  '{objectKey:$k,contentType:"application/pdf",sizeBytes:12345,sha256Hex:"abc123"}')"
code="$(status)"; ATT1="$(location_id)"
[[ "$code" == "201" && -n "$ATT1" ]] || { err "Add attachment failed (code=$code)"; show_body; exit 1; }
ok "Attachment ATT1=$ATT1"

note "List attachments"
GET "/api/exams/$EX1/attachments"; code="$(status)"
[[ "$code" == "200" ]] || { err "List attachments failed ($code)"; show_body; exit 1; }
ok "Attachments listed"

# --- Reporting (CSV) ---
note "Export patients CSV"
CSV="$OUT_DIR/patients.csv"
curl -sS "$BASE/api/reporting/patients/export.csv" -o "$CSV"
[[ -s "$CSV" ]] || { err "CSV export empty"; exit 1; }
ok "CSV saved to $CSV"
head -n 5 "$CSV" | sed 's/^/   /'

ok "All smoke tests passed."
