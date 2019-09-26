package nettyFoutoRefactor.network;

import network.Host;

public interface IMessageConsumer<T> {

    void deliverMessage(byte msgCode, T msg, Host from);
}
