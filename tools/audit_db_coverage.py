# -*- coding: utf-8 -*-
"""Static database coverage audit for BiteSmart.

This script compares SQL table definitions with backend source references.
It is a static audit: it does not connect to MySQL and does not prove runtime
data correctness. Use it together with API smoke tests for real integration.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BACKEND_ROOT = ROOT / "BiteSmart" / "src" / "main"
ROOT_SQL = ROOT / "bitesmart.sql"
MIGRATION_DIRS = [ROOT / "docs" / "db", ROOT / "docs" / "sql"]

SOURCE_EXTS = {".java", ".xml", ".yml", ".yaml", ".properties"}


def unique(items: list[str]) -> list[str]:
    seen = set()
    out = []
    for item in items:
        if item not in seen:
            seen.add(item)
            out.append(item)
    return out


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def table_names_from_sql(text: str) -> list[str]:
    patterns = [
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`([^`]+)`",
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\s*\(",
    ]
    names: list[str] = []
    for pattern in patterns:
        names.extend(re.findall(pattern, text, flags=re.IGNORECASE))
    return unique(names)


def camel_name(table: str) -> str:
    return "".join(part.capitalize() for part in table.split("_"))


def source_files() -> list[Path]:
    files: list[Path] = []
    for path in BACKEND_ROOT.rglob("*"):
        if path.is_file() and path.suffix.lower() in SOURCE_EXTS:
            files.append(path)
    return files


def source_hits(table: str, files: list[Path]) -> list[Path]:
    camel = camel_name(table)
    hits: list[Path] = []
    for path in files:
        text = read_text(path)
        if table in text or camel in text:
            hits.append(path)
    return hits


def mapper_table_references(files: list[Path]) -> list[str]:
    refs: list[str] = []
    sql_keywords = r"(?:FROM|JOIN|INTO|UPDATE|TABLE|DELETE\s+FROM|INSERT\s+INTO)"
    for path in files:
        if path.suffix.lower() != ".xml":
            continue
        text = read_text(path)
        refs.extend(re.findall(sql_keywords + r"\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?", text, flags=re.IGNORECASE))
    noise = {"set", "select", "id"}
    return unique([r for r in refs if r.lower() not in noise])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=None, help="Optional markdown report path.")
    args = parser.parse_args()

    root_sql_text = read_text(ROOT_SQL) if ROOT_SQL.exists() else ""
    root_tables = table_names_from_sql(root_sql_text)

    migration_tables: list[str] = []
    for directory in MIGRATION_DIRS:
        if not directory.exists():
            continue
        for path in sorted(directory.glob("*.sql")):
            migration_tables.extend(table_names_from_sql(read_text(path)))
    migration_tables = unique(migration_tables)

    all_declared_tables = unique(root_tables + migration_tables)
    files = source_files()
    hits_by_table = {table: source_hits(table, files) for table in all_declared_tables}
    unused_declared = [table for table, hits in hits_by_table.items() if not hits]

    mapper_refs = mapper_table_references(files)
    declared_set = set(all_declared_tables)
    mapper_refs_not_declared = [table for table in mapper_refs if table not in declared_set]

    partially_integrated = []
    if "dish_nutrition" in all_declared_tables:
      exact_hits = [path for path in files if "dish_nutrition" in read_text(path)]
      if not exact_hits:
          partially_integrated.append("dish_nutrition")

    lines: list[str] = []
    lines.append("# BiteSmart DB Coverage Audit")
    lines.append("")
    lines.append(f"- Root SQL tables: {len(root_tables)}")
    lines.append(f"- Migration SQL tables: {len(migration_tables)}")
    lines.append(f"- Declared unique tables: {len(all_declared_tables)}")
    lines.append(f"- Backend source files scanned: {len(files)}")
    lines.append("")
    lines.append("## Declared Tables With No Backend Hits")
    lines.append("")
    if unused_declared:
        for table in unused_declared:
            lines.append(f"- `{table}`")
    else:
        lines.append("- None")
    lines.append("")
    lines.append("## Partially Integrated Tables")
    lines.append("")
    if partially_integrated:
        for table in partially_integrated:
            lines.append(f"- `{table}`: declared in SQL, but no exact table reference was found in backend mappers/services.")
    else:
        lines.append("- None")
    lines.append("")
    lines.append("## Mapper Table References Missing From SQL Set")
    lines.append("")
    if mapper_refs_not_declared:
        for table in mapper_refs_not_declared:
            lines.append(f"- `{table}`")
    else:
        lines.append("- None")
    lines.append("")
    lines.append("## Table Hit Counts")
    lines.append("")
    lines.append("| Table | Backend hits |")
    lines.append("|---|---:|")
    for table in all_declared_tables:
        lines.append(f"| `{table}` | {len(hits_by_table[table])} |")
    lines.append("")
    lines.append("## Notes")
    lines.append("")
    lines.append("- Static hits are not runtime proof. Run API smoke tests against a real MySQL database for integration verification.")
    lines.append("- Root SQL alone is not enough if mapper references are introduced by migration SQL.")

    output = "\n".join(lines) + "\n"
    print(output)

    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(output, encoding="utf-8")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
