"""Offline parser tests; these do not connect to NLECloud or PostgreSQL."""
import importlib.util
import os
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import patch


def load_sync():
    # Parsing has no dependency on HTTP/database clients. Stub them only in the
    # isolated import so a reader can run these tests with Python's stdlib.
    spec = importlib.util.spec_from_file_location(
        "sync_under_test", Path(__file__).resolve().parents[1] / "sync.py"
    )
    module = importlib.util.module_from_spec(spec)
    with patch.dict(os.environ, {"DEVICE_ID": "test-device", "DEVICE_NAME": "test-station"}):
        with patch.dict(sys.modules, {
            "requests": types.ModuleType("requests"),
            "psycopg": types.ModuleType("psycopg"),
        }):
            spec.loader.exec_module(module)
    return module


SYNC = load_sync()


def points(times=("2026-10-23T11:40:00",)):
    return [
        {"ApiTag": tag, "PointDTO": [
            {"RecordTime": timestamp, "Value": value} for timestamp in times
        ]}
        for tag, value in [("Temperature", "23.0"), ("Humidity", 62.5), ("Light", 473)]
    ]


class ParseRowsTest(unittest.TestCase):
    def test_complete_shuffled_tags_and_china_timezone(self):
        rows = SYNC.parse_rows(list(reversed(points())))
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0][:4], ("test-station", 23.0, 62.5, 473.0))
        self.assertEqual(rows[0][4].utcoffset().total_seconds(), 8 * 3600)

    def test_different_timestamps_are_not_merged(self):
        data = points()
        data[-1]["PointDTO"][0]["RecordTime"] = "2026-10-23T11:40:01"
        self.assertEqual(SYNC.parse_rows(data), [])

    def test_missing_or_null_metric_drops_incomplete_row(self):
        self.assertEqual(SYNC.parse_rows(points()[:2]), [])
        data = points()
        data[1]["PointDTO"][0]["Value"] = None
        self.assertEqual(SYNC.parse_rows(data), [])

    def test_only_common_times_are_sorted(self):
        data = points(("2026-10-23T11:41:00", "2026-10-23T11:40:00"))
        data[0]["PointDTO"].append({"RecordTime": "2026-10-23T11:42:00", "Value": 24})
        rows = SYNC.parse_rows(data)
        self.assertEqual(len(rows), 2)
        self.assertLess(rows[0][4], rows[1][4])

    def test_explicit_timezone_is_preserved(self):
        rows = SYNC.parse_rows(points(("2026-10-23T03:40:00+00:00",)))
        self.assertEqual(rows[0][4].utcoffset().total_seconds(), 0)

    def test_bad_value_or_time_is_not_silently_zero(self):
        data = points()
        data[0]["PointDTO"][0]["Value"] = "invalid"
        with self.assertRaises(ValueError):
            SYNC.parse_rows(data)
        with self.assertRaises(ValueError):
            SYNC.parse_rows(points(("not-a-time",)))

    def test_empty_input_does_not_open_database(self):
        self.assertEqual(SYNC.parse_rows([]), [])
        self.assertEqual(SYNC.write_rows([]), 0)


if __name__ == "__main__":
    unittest.main()
