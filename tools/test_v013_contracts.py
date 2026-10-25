"""Release/privacy invariants not covered by a simulated handset."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ANDROID='{http://schemas.android.com/apk/res/android}'

class VersionThirteenContracts(unittest.TestCase):
    def test_timer_has_only_standard_notification_and_no_privileged_wake_permissions(self):
        manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
        permissions={p.attrib[ANDROID+'name'] for p in manifest.findall('uses-permission')}
        self.assertIn('android.permission.POST_PROMOTED_NOTIFICATIONS',permissions)
        self.assertTrue(permissions.isdisjoint({'android.permission.USE_FULL_SCREEN_INTENT',
            'android.permission.TURN_SCREEN_ON','android.permission.SYSTEM_ALERT_WINDOW',
            'android.permission.SCHEDULE_EXACT_ALARM','android.permission.USE_EXACT_ALARM'}))
        service=manifest.find('application/service')
        self.assertEqual('false',service.attrib[ANDROID+'exported'])

    def test_brazilian_locale_is_kept_offline_and_purchase_backup_policy_is_unchanged(self):
        config=ET.parse(ROOT/'app/src/main/res/xml/locales_config.xml').getroot()
        locales={p.attrib[ANDROID+'name'] for p in config}
        self.assertIn('pt-BR',locales);self.assertNotIn('pt-PT',locales)
        self.assertTrue((ROOT/'app/src/main/res/values-pt-rBR/strings.xml').is_file())
        self.assertIn('language { enableSplit = false }',(ROOT/'app/build.gradle.kts').read_text())
        for name in ('hmyzak_backup_rules.xml','hmyzak_data_extraction_rules.xml'):
            tree=ET.parse(ROOT/'app/src/main/res/xml'/name)
            paths={p.attrib['path'] for p in tree.iter('include')}
            self.assertEqual({'rezim_local_v1.db','hmyzak_prefs.xml'},paths)

if __name__=='__main__':unittest.main()
