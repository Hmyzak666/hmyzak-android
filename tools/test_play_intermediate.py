import tempfile
import unittest
import zipfile
from pathlib import Path
from verify_play_intermediate import ROOT, verify_config, verify_bundle

class PlayIntermediateTest(unittest.TestCase):
    def config(self):
        key = (ROOT / "config/play-billing-public-key.txt").read_text().strip()
        return f'''public static final boolean PLAY_DISTRIBUTION = true;
public static final boolean TEST_PRO_UNLOCK = false;
public static final boolean DEBUG = false;
public static final String APPLICATION_ID = "cz.rezim.app";
public static final int VERSION_CODE = 13038;
public static final String VERSION_NAME = "0.13.38";
public static final String PLAY_BILLING_PUBLIC_KEY = "{key}";'''

    def test_expected_production_configuration_passes(self):
        verify_config(self.config())

    def test_test_unlock_wrong_identity_and_missing_billing_key_are_rejected(self):
        replacements = [("TEST_PRO_UNLOCK = false", "TEST_PRO_UNLOCK = true"),
                        ("PLAY_DISTRIBUTION = true", "PLAY_DISTRIBUTION = false"),
                        ("DEBUG = false", "DEBUG = true"),
                        ("cz.rezim.app", "cz.rezim.other"),
                        ("13038", "13000"), ("0.13.38", "0.13.0"),
                        ("PLAY_BILLING_PUBLIC_KEY", "OTHER_KEY")]
        for old, new in replacements:
            with self.subTest(setting=old), self.assertRaises(ValueError):
                verify_config(self.config().replace(old, new))

    def test_signed_or_non_bundle_input_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "input.aab"
            for extra in (None, "META-INF/CERT.RSA", "META-INF/UPLOAD.SF", "META-INF/KEY.EC"):
                with zipfile.ZipFile(path, "w") as bundle:
                    bundle.writestr("base/manifest/AndroidManifest.xml", b"manifest")
                    bundle.writestr("BundleConfig.pb", b"config")
                    if extra:
                        bundle.writestr(extra, b"signature")
                if extra:
                    with self.assertRaises(ValueError):
                        verify_bundle(path)
                else:
                    verify_bundle(path)
            with zipfile.ZipFile(path, "w") as bundle:
                bundle.writestr("file.txt", "not a bundle")
            with self.assertRaises(ValueError):
                verify_bundle(path)
