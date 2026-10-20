"""Version and signed-APK compatibility gates; uses only the Python standard library."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def read_properties(path):
    return dict(line.strip().split("=", 1) for line in Path(path).read_text().splitlines()
                if line.strip() and not line.lstrip().startswith("#"))


def release_version(properties, run_number, floor):
    name = properties["versionName"]
    base = int(properties["versionBaseCode"])
    if not re.fullmatch(r"\d+\.\d+", name) or base <= 0 or run_number <= 0:
        raise ValueError("Invalid release version settings or workflow run number")
    code = base + run_number
    if code <= floor:
        raise ValueError(f"versionCode {code} must exceed the installed baseline {floor}")
    return {"HMYZAK_VERSION_CODE": str(code), "HMYZAK_VERSION_NAME": f"{name}.{run_number}",
            "HMYZAK_SHORT_VERSION": name}


def parse_badging(text):
    lines = text.splitlines()
    if not lines or not lines[0].startswith("package: "):
        raise ValueError("aapt did not return package metadata")
    values = dict(re.findall(r"(\w+)='([^']*)'", lines[0]))
    return {"application_id": values["name"], "version_code": int(values["versionCode"]),
            "version_name": values["versionName"]}


def check_metadata(metadata, signers, profile, allow_baseline=False):
    if metadata["application_id"] != profile["application_id"]:
        raise ValueError("APK applicationId differs from the installed app")
    if signers != [profile["signer_sha256"]]:
        raise ValueError("APK signing certificate differs from the installed app")
    floor = profile["minimum_installed_version_code"]
    if metadata["version_code"] < floor or (metadata["version_code"] == floor and not allow_baseline):
        raise ValueError("APK versionCode is not higher than the installed baseline")


def verify_apk(apk, profile, build_tools, allow_baseline=False):
    apk = Path(apk)
    suffix = ".bat" if os.name == "nt" else ""
    signed = subprocess.check_output(
        [str(build_tools / ("apksigner" + suffix)), "verify", "--verbose", "--print-certs", str(apk)],
        text=True, stderr=subprocess.STDOUT)
    signers = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)", signed)
    aapt = build_tools / ("aapt.exe" if os.name == "nt" else "aapt")
    metadata = parse_badging(subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True))
    check_metadata(metadata, signers, profile, allow_baseline)
    if not allow_baseline:
        expected_code = os.environ.get("HMYZAK_VERSION_CODE")
        expected_name = os.environ.get("HMYZAK_VERSION_NAME")
        if expected_code and metadata["version_code"] != int(expected_code):
            raise ValueError("APK versionCode differs from the configured CI version")
        if expected_name and metadata["version_name"] != expected_name:
            raise ValueError("APK versionName differs from the configured CI version")
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if allow_baseline and digest != profile["baseline"]["apk_sha256"]:
        raise ValueError("Baseline APK checksum differs from the preserved release")
    return {**metadata, "signer_sha256": signers[0], "apk_sha256": digest,
            "signature_verified": True, "baseline_only": allow_baseline}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    version = sub.add_parser("version")
    version.add_argument("--run-number", type=int, required=True)
    version.add_argument("--github-env", type=Path)
    verify = sub.add_parser("verify")
    verify.add_argument("--apk", type=Path, required=True)
    verify.add_argument("--build-tools", type=Path)
    verify.add_argument("--report", type=Path)
    verify.add_argument("--allow-baseline", action="store_true",
                        help="Verify only the exact preserved v0.7.14 APK; never used for new releases")
    args = parser.parse_args()
    profile = json.loads((ROOT / "config/release-compatibility.json").read_text())
    if args.command == "version":
        values = release_version(read_properties(ROOT / "app/release-version.properties"),
                                 args.run_number, profile["minimum_installed_version_code"])
        text = "".join(f"{key}={value}\n" for key, value in values.items())
        if args.github_env:
            with args.github_env.open("a") as out:
                out.write(text)
        print(text, end="")
    else:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        build_tools = args.build_tools or (Path(sdk) / "build-tools/35.0.0" if sdk else None)
        if build_tools is None:
            parser.error("Pass --build-tools or set ANDROID_HOME")
        result = verify_apk(args.apk, profile, build_tools, args.allow_baseline)
        text = json.dumps(result, indent=2) + "\n"
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(text)
        print(text, end="")


if __name__ == "__main__":
    main()
