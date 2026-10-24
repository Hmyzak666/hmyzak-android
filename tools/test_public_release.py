"""Backup policy and additive schema 10 -> 11 regression checks."""
from pathlib import Path
import re,sqlite3,unittest,xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'
A='{http://schemas.android.com/apk/res/android}'
class PublicReleasePolicyTest(unittest.TestCase):
    def test_both_backup_transports_include_only_database_and_permanent_preferences(self):
        root=E.parse(MAIN/'res/xml/hmyzak_data_extraction_rules.xml').getroot()
        self.assertEqual({'cloud-backup','device-transfer'},{n.tag for n in root})
        self.assertEqual('true',root.find('cloud-backup').get('disableIfNoEncryptionCapabilities'))
        expected={('database','rezim_local_v1.db'),('sharedpref','hmyzak_prefs.xml')}
        for n in root:
            self.assertEqual(expected,{(i.get('domain'),i.get('path')) for i in n.findall('include')})
        old=E.parse(MAIN/'res/xml/hmyzak_backup_rules.xml').getroot()
        self.assertEqual(expected,{(i.get('domain'),i.get('path')) for i in old.findall('include')})
    def test_manifest_enables_full_backup_without_backing_up_foreground_writers(self):
        app=E.parse(MAIN/'AndroidManifest.xml').getroot().find('application')
        self.assertEqual('true',app.get(A+'allowBackup'))
        self.assertEqual('true',app.get(A+'fullBackupOnly'))
        self.assertEqual('false',app.get(A+'backupInForeground'))
        self.assertEqual('.LiftAntBackupAgent',app.get(A+'backupAgent'))
        self.assertEqual('@xml/hmyzak_backup_rules',app.get(A+'fullBackupContent'))
        self.assertEqual('@xml/hmyzak_data_extraction_rules',app.get(A+'dataExtractionRules'))
    def test_real_upgrade_adds_only_activity_and_setup_metadata(self):
        db=sqlite3.connect(':memory:')
        db.executescript((ROOT/'app/src/test/resources/fixtures/schema-v10.sql').read_text())
        db.execute("INSERT INTO plan_days VALUES ('d','D','Keep',0)")
        db.execute("INSERT INTO plan_exercises VALUES ('x','d','Keep',4,'KG',0,NULL,'Note','g',4,2)")
        db.execute("INSERT INTO sessions VALUES (10,'d',100,1000,900,3)")
        db.execute("INSERT INTO hiit VALUES (11,'2026-10-20',20,8,30,90,140,160,'Keep',2.5,3,2,8,3)")
        before={t:db.execute('SELECT * FROM '+t).fetchall() for t in ('plan_days','plan_exercises','sessions','hiit')}
        source=(MAIN/'java/cz/rezim/app/LocalStore.kt').read_text()
        block=source.split('private fun addPublicReleaseSchema',1)[1].split('\n    }',1)[0]
        statements=re.findall(r'db.execSQL\("([^"\n]+)"',block)
        self.assertEqual(3,len(statements))
        for sql in statements: db.execute(sql,('1',) if '?' in sql else ())
        for table,rows in before.items():
            after=db.execute('SELECT * FROM '+table).fetchall()
            self.assertEqual(rows,[r[:-1] for r in after] if table=='hiit' else after)
        self.assertEqual(('OTHER',),db.execute('SELECT activity FROM hiit').fetchone())
        self.assertEqual(('1',),db.execute("SELECT value FROM app_state WHERE id='setup_complete'").fetchone())
        self.assertEqual(('ok',),db.execute('PRAGMA integrity_check').fetchone())
    def test_clean_install_does_not_seed_authors_plan(self):
        source=(MAIN/'java/cz/rezim/app/LocalStore.kt').read_text()
        create=source.split('override fun onCreate',1)[1].split('override fun onUpgrade',1)[0]
        self.assertNotIn('seedPlans(db)',create)
        self.assertIn('addPublicReleaseSchema(db, false)',create)
    def test_public_build_cannot_enable_test_pro(self):
        source=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('require(!liftantTestPro)',source)
        self.assertIn('val liftantTestPro = providers.gradleProperty("liftantTestPro").orNull == "true"',source)
        self.assertIn('-PliftantTestPro=true',(ROOT/'.github/workflows/android-release.yml').read_text())
    def test_no_fixed_prices_or_subscription_sku_in_billing_code(self):
        source=(MAIN/'java/cz/rezim/app/ProBilling.kt').read_text()
        self.assertNotIn('ProductType.SUBS',source)
        self.assertNotIn('consumeAsync',source)
        self.assertIn('formattedPrice',source)
        for price in ('199 Kč','8.99','7.99'):self.assertNotIn(price,source)
if __name__=='__main__':unittest.main()
