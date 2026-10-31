"""Validate the actual generated Play build before handing it to the owner for signing."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def verify_config(text):
    required = {
        "PLAY_DISTRIBUTION": "true", "TEST_PRO_UNLOCK": "false", "DEBUG": "false",
        "APPLICATION_ID": '\"cz.rezim.app\"', "VERSION_CODE": "13038",
        "VERSION_NAME": '\"0.13.38\"',
    }
    for key, value in required.items():
        if not re.search(r"\b" + key + r"\s*=\s*" + re.escape(value) + r"\s*;", text):
            raise ValueError(f"Invalid generated build setting: {key}")
    public_key = (ROOT / "config/play-billing-public-key.txt").read_text().strip()
    if not public_key or f'PLAY_BILLING_PUBLIC_KEY = "{public_key}";' not in text:
        raise ValueError("Real Billing public key missing from generated configuration")


def verify_bundle(path):
    with zipfile.ZipFile(path) as bundle:
        names = bundle.namelist()
        if bundle.testzip() is not None:
            raise ValueError("Corrupt bundle")
        if "base/manifest/AndroidManifest.xml" not in names or "BundleConfig.pb" not in names:
            raise ValueError("Not an Android App Bundle")
        if any(n.upper().startswith("META-INF/") and n.upper().endswith((".RSA", ".DSA", ".EC", ".SF")) for n in names):
            raise ValueError("External-signing input must be unsigned")


def main():
    configs = list((ROOT / "app/build/generated/source/buildConfig/release").rglob("BuildConfig.java"))
    if len(configs) != 1:
        raise ValueError("Expected one release BuildConfig")
    verify_config(configs[0].read_text())
    manifests = list((ROOT / "app/build/intermediates/merged_manifests/release").rglob("AndroidManifest.xml"))
    if len(manifests) != 1:
        raise ValueError("Expected one merged release manifest")
    manifest = ET.parse(manifests[0]).getroot()
    assert manifest.get("package") == "cz.rezim.app"
    assert manifest.get(ANDROID + "versionCode") == "13038"
    assert manifest.find("application").get(ANDROID + "debuggable", "false") == "false"
    assert "com.android.vending.BILLING" in {p.get(ANDROID + "name") for p in manifest.findall("uses-permission")}
    source = ROOT / "app/build/outputs/bundle/release/app-release.aab"
    verify_bundle(source)
    target_dir = ROOT / "app/build/outputs/play-external"
    target_dir.mkdir(parents=True, exist_ok=True)
    target = target_dir / "LiftAnt-0.13.38-13038-UNSIGNED.aab"
    shutil.copyfile(source, target)
    metadata = {
        "status": "UNSIGNED: owner must sign before Play upload",
        "applicationId": "cz.rezim.app", "versionCode": 13038, "versionName": "0.13.38",
        "testProUnlock": False, "playDistribution": True,
        "sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
        "expectedUploadCertificateSha256": "EAF5175CB25A4C933D4D502DC0214D393CA3191A1AD55332223B4BDE32991193",
    }
    (target_dir / "build-verification.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
