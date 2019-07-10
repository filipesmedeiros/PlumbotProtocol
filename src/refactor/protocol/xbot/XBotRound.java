package refactor.protocol.xbot;

import java.net.InetSocketAddress;

class XBotRound {
	
	enum Role {
		initiator,
		candidate,
		old,
		disconnected
	}

	private InetSocketAddress initiator;
	private InetSocketAddress old;
	private InetSocketAddress candidate;
	private InetSocketAddress disconnected;

	private long itoc;
	private long itoo;
	private long dtoc;
	private long dtoo;

	public InetSocketAddress initiator() {
		return initiator;
	}

	public XBotRound initiator(InetSocketAddress initiator) {
		this.initiator = initiator;
		return this;
	}

	public InetSocketAddress candidate() {
		return candidate;
	}

	public XBotRound candidate(InetSocketAddress candidate) {
		this.candidate = candidate;
		return this;
	}

	public InetSocketAddress old() {
		return old;
	}

	public XBotRound old(InetSocketAddress old) {
		this.old = old;
		return this;
	}

	public InetSocketAddress disconnected() {
		return disconnected;
	}

	public XBotRound disconnected(InetSocketAddress disconnected) {
		this.disconnected = disconnected;
		return this;
	}

	public long itoc() {
		return itoc;
	}

	public XBotRound itoc(long itoc) {
		this.itoc = itoc;
		return this;
	}

	public long itoo() {
		return itoo;
	}

	public XBotRound itoo(long itoo) {
		this.itoo = itoo;
		return this;
	}

	public long dtoc() {
		return dtoc;
	}

	public XBotRound dtoc(long dtoc) {
		this.dtoc = dtoc;
		return this;
	}

	public long dtoo() {
		return dtoo;
	}

	public XBotRound dtoo(long dtoo) {
		this.dtoo = dtoo;
		return this;
	}
}
