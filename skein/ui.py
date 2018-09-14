from __future__ import absolute_import, print_function, division

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


ProxyInfo = namedtuple('ProxyInfo', ['proxy_info', 'proxy_hosts', 'proxy_uri_bases'])


class WebUI(object):
    """The Skein WebUI."""
    def __init__(self, client):
        # The application client
        self._client = client

    @cached_property
    def _proxy_info(self):
        resp = self._client._call('proxy_info', proto.ProxyInfoRequest())
        return ProxyInfo(resp.proxy_prefix,
                         tuple(resp.proxy_host),
                         tuple(resp.proxy_uri_base))

    @property
    def proxy_prefix(self):
        return self._proxy_info.proxy_prefix

    @property
    def proxy_hosts(self):
        return self._proxy_info.proxy_hosts

    @property
    def proxy_uri_bases(self):
        return self._proxy_info.proxy_uri_bases

    def get_proxies(self):
        resp = self._client._call('get_proxies', proto.GetProxiesRequest())
        return [ProxiedPage(i.prefix, i.target, i.name if i.name else None)
                for i in resp.proxy]

    def get_proxies_by_name(self):
        return {p.name: p for p in self.get_proxies() if p.name is not None}

    def get_proxies_by_prefix(self):
        return {p.prefix: p for p in self.get_proxies()}

    def add_proxy(self, prefix, target, name=None):
        req = proto.AddProxyRequest(prefix=prefix, target=target, name=name)
        self._client._call('add_proxy', req)

    def remove_proxy(self, prefix):
        req = proto.RemoveProxyRequest(prefix=prefix)
        self._client._call('remove_proxy', req)
