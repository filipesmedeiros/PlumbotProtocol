package networkFouto.network;

import network.Host;

public interface INodeListener
{
    void nodeDown(Host peer);

    void nodeUp(Host peer);

    void nodeConnectionReestablished(Host peerHost);
}
