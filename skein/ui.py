from __future__ import absolute_import, print_function, division

import random
from collections import namedtuple

from . import proto
from .utils import cached_property


__all__ = ('ProxiedPage', 'WebUI')


class ProxiedPage(namedtuple('ProxiedPage', ['prefix', 'target', 'name'])):
    """A page proxied by the Skein Web UI.

    Parameters
    ----------
    prefix : string
        The prefix used in the address to the proxied page.
    target : string
        The target address of the proxy.
    name : string or None, optional
        The name to link the page to in the Web UI. If no name is provided, the
        page is still proxied but won't be linked to in the Web UI.
    """
    def __new__(cls, prefix, target, name=None):
        return super(ProxiedPage, cls).__new__(cls, prefix, target, name)


UIInfo = namedtuple('UIInfo', ['ui_addresses', 'proxy_prefix'])


class WebUI(object):
    """The Skein WebUI."""
    def __init__(self, client):
        # The application client
        self._client = client

    @cached_property
    def _ui_info(self):
        resp = self._client._call('UiInfo', proto.UIInfoRequest())
        return UIInfo(tuple(resp.ui_address), resp.proxy_prefix)

    @property
    def proxy_prefix(self):
        return self._ui_info.proxy_prefix

    @property
    def addresses(self):
        return self._ui_info.ui_addresses

    @property
    def address(self):
        return random.choice(self._ui_info.ui_addresses)

    def get_proxies(self):
        resp = self._client._call('GetProxies', proto.GetProxiesRequest())
        return [ProxiedPage(i.prefix, i.target, i.name if i.name else None)
                for i in resp.proxy]

    def get_proxies_by_name(self):
        return {p.name: p for p in self.get_proxies() if p.name is not None}

    def get_proxies_by_prefix(self):
        return {p.prefix: p for p in self.get_proxies()}

    def add_proxy(self, prefix, target, name=None):
        req = proto.Proxy(prefix=prefix, target=target, name=name)
        self._client._call('AddProxy', req)

    def remove_proxy(self, prefix):
        req = proto.RemoveProxyRequest(prefix=prefix)
        self._client._call('RemoveProxy', req)
