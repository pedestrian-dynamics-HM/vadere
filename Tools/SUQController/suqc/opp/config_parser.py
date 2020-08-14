import enum
from collections.abc import MutableMapping
from configparser import ConfigParser, NoOptionError


class OppParser(ConfigParser):
    def optionxform(self, optionstr):
        return optionstr


class OppConfigType(enum.Enum):
    """
    Set type on OppConfigFileBase to create read-only configurations if needed.
    """

    READ_ONLY = 1
    EDIT_LOCAL = 2
    EXT_DEL_LOCAL = 3

    def __lt__(self, other):
        if self.__class__ is other.__class__:
            return self.value < other.value
        elif type(other) is int:
            return self.value < other
        return NotImplemented

    def __gt__(self, other):
        return self != other and other < self

    def __eq__(self, other):
        if self.__class__ is other.__class__:
            return self.value == other.value
        elif type(other) is int:
            return self.value == other
        return NotImplemented

    def __le__(self, other):
        return self < other or self == other

    def __ge__(self, other):
        return self > other or self == other


class OppConfigFileBase(MutableMapping):
    """
    Represents an omnetpp.ini file. The extends logic is defined in SimulationManual.pdf p.282 ff.
    Each OppConfigFileBase object has a reference to complete omnetpp.ini configuration file but
    only access's its own options, as well as all options reachable by the search path build
    using the 'extends' option.

    Example(taken form [1]):
    The search path for options for the configuration `SlottedAloha2b` is:
    SlottedAloha2b->SlottedAloha2->SlottedAlohaBase->HighTrafficSettings->General
    ```
    [General]
    ...
    [Config SlottedAlohaBase]
    ...
    [Config LowTrafficSettings]
    ...
    [Config HighTrafficSettings]
    ...

    [Config SlottedAloha1]
    extends = SlottedAlohaBase, LowTrafficSettings
    ...
    [Config SlottedAloha2]
    extends = SlottedAlohaBase, HighTrafficSettings
    ...
    [Config SlottedAloha2a]
    extends = SlottedAloha2
    ...
    [Config SlottedAloha2b]
    extends = SlottedAloha2
    ```
    [1]: https://doc.omnetpp.org/omnetpp/manual/#sec:config-sim:section-inheritance
    """

    @classmethod
    def from_path(
        cls, ini_path, config, cfg_type=OppConfigType.EDIT_LOCAL, is_parent=False
    ):
        _root = OppParser(inline_comment_prefixes="#")
        _root.read(ini_path)
        return cls(_root, config, cfg_type, is_parent)

    def __init__(
        self,
        root_cfg: OppParser,
        config_name: str,
        cfg_type=OppConfigType.EDIT_LOCAL,
        is_parent=False,
    ):
        self._root: OppParser = root_cfg
        self._cfg_type = cfg_type
        self._sec = self._ensure_config_prefix(config_name)

        if not self._has_section_(self._sec):
            raise ValueError(f"no section found with name {self._sec}")
        self._parent_cfg = []
        self.section_hierarchy = [self._ensure_config_prefix(self._sec)]
        self._is_parent = is_parent
        if not self._is_parent:
            stack = [iter(self.parents)]
            while stack:
                for p in stack[0]:
                    if p == "":
                        continue
                    _pp = OppConfigFileBase(self._root, p, is_parent=True)
                    self._parent_cfg.append(_pp)
                    self.section_hierarchy.append(self._ensure_config_prefix(p))
                    if len(_pp.parents) > 0:
                        stack.append(iter(_pp.parents))
                else:
                    stack.pop(0)

        if config_name != "General" and self._has_section_("General"):
            self._parent_cfg.append(
                OppConfigFileBase(self._root, "General", is_parent=True)
            )
            self.section_hierarchy.append("General")

    def writer(self, fp):
        """ write the current state to the given file descriptor. Caller must close file."""
        self._root.write(fp)

    @staticmethod
    def _ensure_config_prefix(val):
        """ All omnetpp configurations start with 'Config'. Add 'Config' if it is missing.  """
        if not val.startswith("Config") and val != "General":
            return f"Config {val.strip()}"
        return val

    @property
    def section(self):
        """ Section managed by this OppConfigFileBase object (read-only) """
        return self._sec

    @property
    def parents(self):
        """ local parents i.e all configurations listed in the 'extends' option (read-only) """
        return [
            s.strip()
            for s in self._getitem_local_("extends", default="").strip().split(",")
        ]

    @property
    def type(self):
        return self._cfg_type

    def is_local(self, option):
        """
        Returns True if the given object belongs directly to the current section and False if
        options is contained higher up the hierarchy OR does not exist.
        """
        return self._contains_local_(option)

    def get_config_for_option(self, option):
        """
        Returns the name of the section the option first occurs search order: local --> general
        or None if option does not exist
        """
        if self._contains_local_(option):
            return self.section
        else:
            for p in self._parent_cfg:
                if p._contains_local_(option):
                    return p.get_config_for_option(option)
        return None

    def _has_section_(self, sec):
        """
        True if section exist in the configuration. Note: Returns also True even if given section is not
        in the section_hierarchy if the current section.
        """
        return self._root.has_section(sec)

    def _getitem_local_(self, k, default=None):
        """
        Search for key in local configuration
        """
        try:
            return self._root.get(self._sec, k)
        except NoOptionError:
            if default is not None:
                return default
            else:
                raise KeyError(f"key not found. Key: {k}")

    def _set_local(self, k, v):
        """
        Set new value for key. OppConfigType checks already done
        """
        self._root.set(self._sec, k, v)

    def _contains_local_(self, k):
        """
        True if key exist in current section (parents are not searched) otherwise False
        """
        return self._root.has_option(self._sec, k)

    def _contained_by_parent(self, k):
        """
        True if key exists in any parent. Note key my exist multiple time but only first occurrence of key
        will be returned. See search path.
        """
        return any([k in parent for parent in self._parent_cfg])

    def _delitem_local(self, k):
        """
        Delete local key.
        """
        self._root.remove_option(self._sec, k)

    def __setitem__(self, k, v):
        if self._cfg_type is OppConfigType.READ_ONLY:
            raise NotImplementedError("Cannot set value on read only config")

        if self._contains_local_(k):
            self._set_local(k, v)
        elif self._contained_by_parent(k):
            if self._cfg_type <= OppConfigType.EDIT_LOCAL:
                raise NotImplementedError("Cannot edit value of parent config")
            else:
                for p in self._parent_cfg:
                    if p._contains_local_(k):
                        p._set_local(k, v)
                        return
        else:
            raise KeyError(f"key not found. Key: {k}")

    def __delitem__(self, k):
        if self._cfg_type.value < OppConfigType.EXT_DEL_LOCAL:
            raise ValueError(
                f"current object does not allow deletion. cfg_type={self._cfg_type}"
            )
        if k not in self:
            raise KeyError(f"key not found. Key: {k}")
        if self._contains_local_(k):
            self._root.remove_option(self._sec, k)
        else:
            raise NotImplementedError(
                f"deletion of parent config option not implemented"
            )

    def __getitem__(self, k):
        if k not in self:
            raise KeyError(f"key not found. Key: {k}")

        if self._contains_local_(k):
            return self._getitem_local_(k)
        else:
            for parent in self._parent_cfg:
                try:
                    return parent._getitem_local_(k)
                except KeyError:
                    pass
        raise KeyError(f"key not found. Key: {k}")

    def __contains__(self, k) -> bool:
        if self._contains_local_(k):
            return True
        elif any([k in parent for parent in self._parent_cfg]):
            return True
        else:
            return False

    def __len__(self) -> int:
        _len = 0
        for s in self.section_hierarchy:
            _len += len(self._root.items(s))
        return _len

    def __iter__(self):
        for s in self.section_hierarchy:
            for item in self._root.items(s):
                yield item

    def items(self):
        return list(self.__iter__())

    def keys(self):
        return [k for k, _ in self.__iter__()]

    def values(self):
        return [v for _, v in self.__iter__()]

    def get(self, k):
        return self[k]

    def setdefault(self, k, default=...):
        if k in self:
            return self[k]
        else:
            if self._cfg_type <= OppConfigType.EDIT_LOCAL:
                raise NotImplementedError(
                    "Cannot set value on READ_ONLY or EDIT_LOCAL config. Use EXT_DEL_LOCAL "
                )
            else:
                self._set_local(k, default)
        return default


class OppConfigFile(OppConfigFileBase):
    """
    Helpers to manage OMNeT++ specifics not part of the standard ini-Configuration
    * Read/Write int and doubles
    * specify units (i.e. s, dBm, m)
    * Handle string quotes (are part of the value)
    todo: implement
    """

    def __init__(self, root_cfg: OppParser, config_name: str):
        super().__init__(root_cfg, config_name)
