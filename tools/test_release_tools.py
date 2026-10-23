import json
from pathlib import Path
import unittest

from release_tools import check_metadata, parse_badging, release_version

PROFILE = json.loads((Path(__file__).resolve().parents[1] / "config/release-compatibility.json").read_text())


class ReleaseCompatibilityTest(unittest.TestCase):
    def test_existing_workflow_counter_produces_an_installable_version(self):
        result = release_version({"versionBaseCode": "7000", "versionName": "0.7"}, 15, 7014)
        self.assertEqual("7015", result["HMYZAK_VERSION_CODE"])
        self.assertEqual("0.7.15", result["HMYZAK_VERSION_NAME"])

    def test_reset_or_reused_workflow_counter_is_rejected(self):
        for run in [0, 1, 14]:
            with self.subTest(run=run), self.assertRaises(ValueError):
                release_version({"versionBaseCode": "7000", "versionName": "0.7"}, run, 7014)

    def test_bad_version_settings_cannot_enter_github_env(self):
        with self.assertRaises(ValueError):
            release_version({"versionBaseCode": "7000", "versionName": "0.7\nOTHER=value"}, 15, 7014)

    def test_package_metadata_is_parsed_from_real_aapt_format(self):
        self.assertEqual({"application_id": "cz.rezim.app", "version_code": 7015, "version_name": "0.7.15"},
                         parse_badging("package: name='cz.rezim.app' versionCode='7015' versionName='0.7.15' platformBuildVersionName='16'\n"))

    def test_changed_application_id_or_signer_is_rejected(self):
        correct = {"application_id": "cz.rezim.app", "version_code": 10019, "version_name": "0.11.22"}
        for package, signers in [("cz.other.app", [PROFILE["signer_sha256"]]),
                                 ("cz.rezim.app", ["0" * 64]), ("cz.rezim.app", [])]:
            with self.subTest(package=package, signers=signers), self.assertRaises(ValueError):
                check_metadata({**correct, "application_id": package}, signers, PROFILE)

    def test_old_apk_cannot_pass_the_new_release_gate(self):
        for code in (7014,7015,8016,8017,9018):
            with self.subTest(code=code), self.assertRaises(ValueError):
                check_metadata({"application_id": "cz.rezim.app", "version_code": code},
                               [PROFILE["signer_sha256"]], PROFILE)

    def test_new_feature_release_exceeds_the_confirmed_installed_version(self):
        result = release_version({"versionBaseCode":"11000","versionName":"0.11"},22,PROFILE["minimum_installed_version_code"])
        self.assertEqual("11022",result["HMYZAK_VERSION_CODE"])
        check_metadata({"application_id":"cz.rezim.app","version_code":11022},[PROFILE["signer_sha256"]],PROFILE)
        check_metadata({"application_id":"cz.rezim.app","version_code":7014},[PROFILE["signer_sha256"]],PROFILE,allow_baseline=True)


if __name__ == "__main__":
    unittest.main()
