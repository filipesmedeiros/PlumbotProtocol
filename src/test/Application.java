package test;

import message.plumtree.BodyMessage;

public interface Application {

    void deliver(BodyMessage message);
}
