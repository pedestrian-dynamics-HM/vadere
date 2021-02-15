import os
import unittest

from suqc.opp.config_parser import OppConfigFileBase, OppConfigType


class OppConfigFileBaseTest(unittest.TestCase):

    NEW_FILE = "omnetpp_2.ini"
    NEW_FILE_COMP = "omnetpp_compare.ini"
    NEW_FILE_DEL = "omnetpp_del.ini"

    @staticmethod
    def get_object(
        config, cfg_type=OppConfigType.EDIT_LOCAL, is_parent=False, path="omnetpp.ini"
    ):
        return OppConfigFileBase.from_path(
            os.path.join(os.path.split(__file__)[0], path), config, cfg_type, is_parent
        )

    @staticmethod
    def save_object(obj: OppConfigFileBase, path):
        with open(os.path.join(os.path.split(__file__)[0], path), "w") as f:
            obj.writer(f)

    @staticmethod
    def get_lines(path):
        with open(os.path.join(os.path.split(__file__)[0], path), "r") as f:
            return f.readlines()

    def tearDown(self) -> None:
        f = os.path.join(os.path.split(__file__)[0], self.NEW_FILE)
        if os.path.exists(f):
            os.remove(f)

    def test_set_default_exits(self):
        opp = self.get_object("HighTrafficSettings", OppConfigType.READ_ONLY)
        # set default on exiting must work
        self.assertEqual(opp.setdefault("opt_3", '"val_3"'), '"val_3"')
        # set new will ignore default on existing
        self.assertEqual(opp.setdefault("opt_3", '"new"'), '"val_3"')

    def test_set_default_not_exits(self):
        opp = self.get_object("HighTrafficSettings", OppConfigType.READ_ONLY)
        # must raise error on OppConfigType.READ_ONLY
        self.assertRaises(NotImplementedError, opp.setdefault, "new_key", "42")
        opp = self.get_object("HighTrafficSettings", OppConfigType.EDIT_LOCAL)
        # must raise error on OppConfigType.EDIT_LOCAL
        self.assertRaises(NotImplementedError, opp.setdefault, "new_key", "42")
        opp = self.get_object("HighTrafficSettings", OppConfigType.EXT_DEL_LOCAL)
        # must work on OppConfigType.EXT_DEL_LOCAL
        ret = opp.setdefault("new_key", "42")
        self.assertEqual(ret, "42")
        self.assertEqual(opp["new_key"], "42")
        # new key must be local
        self.assertTrue(opp.is_local("new_key"))

    def test_read_only(self):
        opp = self.get_object("HighTrafficSettings", OppConfigType.READ_ONLY)
        # reading must work
        self.assertEqual(opp["opt_3"], '"val_3"')
        self.assertEqual(opp["general_option"], '"VAL1"')
        # setting new values must not work for (local and parent options)
        self.assertRaises(NotImplementedError, opp.__setitem__, "opt_3", '"new_val"')
        self.assertRaises(
            NotImplementedError, opp.__setitem__, "general_option", '"new_val"'
        )

    def test_edit_local(self):
        opp = self.get_object("HighTrafficSettings", OppConfigType.EDIT_LOCAL)
        # reading must work
        self.assertEqual(opp["opt_3"], '"val_3"')
        self.assertEqual(opp["general_option"], '"VAL1"')
        # setting new values must work for local options only
        opp["opt_3"] = '"new_val"'
        self.assertEqual(opp["opt_3"], '"new_val"')
        # general_option belongs to parent config 'General'
        self.assertRaises(
            NotImplementedError, opp.__setitem__, "general_option", '"new_val"'
        )

    def test_hierarchy(self):
        """ Ensure correct lookup order for extended configurations"""
        opp = self.get_object("SlottedAloha2b", OppConfigType.EDIT_LOCAL)
        self.assertListEqual(
            opp.section_hierarchy,
            [
                "Config SlottedAloha2b",
                "Config SlottedAloha2",
                "Config SlottedAlohaBase",
                "Config HighTrafficSettings",
                "General",
            ],
        )
        opp = self.get_object("SlottedAloha1", OppConfigType.EDIT_LOCAL)
        self.assertListEqual(
            opp.section_hierarchy,
            [
                "Config SlottedAloha1",
                "Config SlottedAlohaBase",
                "Config LowTrafficSettings",
                "General",
            ],
        )
        opp = self.get_object("General", OppConfigType.EDIT_LOCAL)
        self.assertListEqual(opp.section_hierarchy, ["General"])
        opp = self.get_object("SlottedAlohaBase", OppConfigType.EDIT_LOCAL)
        self.assertListEqual(
            opp.section_hierarchy, ["Config SlottedAlohaBase", "General"]
        )

    def test_override(self):
        opp = self.get_object("SlottedAloha2", OppConfigType.EXT_DEL_LOCAL)
        opp["opt_5"] = '"val_55"'
        self.save_object(opp, self.NEW_FILE)
        opp2 = opp = self.get_object(
            "SlottedAloha2", OppConfigType.EXT_DEL_LOCAL, path=self.NEW_FILE
        )
        self.assertEqual(opp2["opt_5"], '"val_55"')
        lines_new = [
            line for line in self.get_lines(self.NEW_FILE) if not line.startswith("\n")
        ]
        lines_comp = [
            line
            for line in self.get_lines(self.NEW_FILE_COMP)
            if not line.startswith("\n")
        ]
        self.assertListEqual(lines_new, lines_comp)

    def test_safe_to_file(self):
        sa_2 = self.get_object("SlottedAloha2", OppConfigType.EXT_DEL_LOCAL)
        hts = self.get_object("HighTrafficSettings", OppConfigType.EXT_DEL_LOCAL)
        self.assertEqual(sa_2["opt_HT"], '"overwritten_val_HT"')
        self.assertEqual(
            sa_2["general_option"],
            '"general_option option overwritten by SlottedAloha2"',
        )
        self.assertEqual(hts["opt_HT"], '"val_HT"')
        self.assertEqual(hts["general_option"], '"VAL1"')

    def test_delete_key(self):
        opp = self.get_object("SlottedAloha2", OppConfigType.EXT_DEL_LOCAL)
        self.assertEqual(
            opp["general_option"],
            '"general_option option overwritten by SlottedAloha2"',
        )
        del opp["general_option"]
        # after deletion value from General section must be accessible.
        self.assertEqual(opp["general_option"], '"VAL1"')
        self.save_object(opp, self.NEW_FILE)
        lines_new = [
            line for line in self.get_lines(self.NEW_FILE) if not line.startswith("\n")
        ]
        lines_del = [
            line
            for line in self.get_lines(self.NEW_FILE_DEL)
            if not line.startswith("\n")
        ]
        self.assertListEqual(lines_new, lines_del)
