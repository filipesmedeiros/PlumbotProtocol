package refactor.protocol.xbot;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import refactor.GlobalSettings;
import refactor.protocol.xbot.XBotRound.Role;

class XBotRoundPool {
	
	private XBotRound initiatorRound;
	private List<XBotRound> candidateRounds;
	private List<XBotRound> oldRounds;
	private List<XBotRound> disconnectedRounds;
	
	XBotRoundPool() {
		oldRounds = new ArrayList<>();
		candidateRounds = new ArrayList<>();
		disconnectedRounds = new ArrayList<>();
	}

	public XBotRound round(Role myRole, Role otherRole, InetSocketAddress node) {
		EnumSet<Role> roleEnumSet = EnumSet.allOf(Role.class);
		for(Role role1 : roleEnumSet)
			for(Role role2 : roleEnumSet)
				if(myRole == role1 && otherRole == role2)
					for(XBotRound round : candidateRounds)
						if(round.initiator().equals(node))
							return round;
		return null;
	}

	public XBotRound createRound(Role role, InetSocketAddress... nodes) {
		switch(role) {
			case initiator:
				if(initiatorRound() != null)
					return null;
				initiatorRound = new XBotRound();
				initiatorRound.initiator(GlobalSettings.localAddress());
				if(nodes.length >= 1)
					initiatorRound.candidate(nodes[0]);
				if(nodes.length >= 2)
					initiatorRound.old(nodes[1]);
				if(nodes.length == 3)
					initiatorRound.disconnected(nodes[2]);
				return initiatorRound;
			case candidate:
				if(candidateRound(nodes[0]) != null)
					return null;
				XBotRound candidateRound = new XBotRound();
				initiatorRound.candidate(GlobalSettings.localAddress());
				candidateRound.initiator(nodes[0]);
				if(nodes.length >= 2)
					candidateRound.old(nodes[1]);
				if(nodes.length >= 3)
					candidateRound.disconnected(nodes[2]);
				candidateRounds.add(candidateRound);
				return candidateRound;
			case old:
				if(oldRound(nodes[0]) != null)
					return null;
				XBotRound oldRound = new XBotRound();
				initiatorRound.old(GlobalSettings.localAddress());
				oldRound.initiator(nodes[0]);
				if(nodes.length >= 2)
					oldRound.candidate(nodes[1]);
				if(nodes.length >= 3)
					oldRound.disconnected(nodes[2]);
				oldRounds.add(oldRound);
				return oldRound;
			case disconnected:
				if(disconnectedRound(nodes[0]) != null)
					return null;
				XBotRound disconnectedRound = new XBotRound();
				initiatorRound.disconnected(GlobalSettings.localAddress());
				disconnectedRound.initiator(nodes[0]);
				if(nodes.length >= 2)
					disconnectedRound.candidate(nodes[1]);
				if(nodes.length >= 3)
					disconnectedRound.old(nodes[2]);
				disconnectedRounds.add(disconnectedRound);
				return disconnectedRound;
		}
		return null;
	}
	
	public XBotRound initiatorRound() {
		return initiatorRound;
	}
	
	public XBotRound oldRound(InetSocketAddress initiator) {
		return getRound(Role.old, initiator);
	}
	
	public XBotRound candidateRound(InetSocketAddress initiator) {
		return getRound(Role.candidate, initiator);
	}
	
	public XBotRound disconnectedRound(InetSocketAddress initiator) {
		return getRound(Role.disconnected, initiator);
	}
	
	public XBotRound getRound(XBotRound.Role localRole, InetSocketAddress initiator) {
		List<XBotRound> rounds = decideRounds(localRole);

		if(rounds == null)
			return initiatorRound;
		else {
			for(XBotRound round : rounds)
				if(round.initiator().equals(initiator))
					return round;
			
			return null;
		}
	}

	public XBotRound removeRound(Role localRole, InetSocketAddress initiator) {
		List<XBotRound> rounds = decideRounds(localRole);

		if(rounds == null)
			try {
				return initiatorRound;
			} finally {
				initiatorRound = null;
			}
		else {
			for(int i = 0; i < rounds.size(); i++)
				if(rounds.get(i).initiator().equals(initiator))
					return rounds.remove(i);

			return null;
		}
	}

	private List<XBotRound> decideRounds(Role localRole) {
		List<XBotRound> rounds = null;
		if(localRole == Role.old)
			rounds = oldRounds;
		else if(localRole == Role.candidate)
			rounds = candidateRounds;
		else if(localRole == Role.disconnected)
			rounds = disconnectedRounds;

		return rounds;
	}
}
