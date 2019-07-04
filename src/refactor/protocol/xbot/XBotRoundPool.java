package refactor.protocol.xbot;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import refactor.protocol.xbot.XBotRound.Role;

class XBotRoundPool {
	
	private XBotRound initiatorRound;
	private List<XBotRound> oldRounds;
	private List<XBotRound> candidateRounds;
	private List<XBotRound> disconnectedRounds;
	
	XBotRoundPool() {
		oldRounds = new ArrayList<>();
		candidateRounds = new ArrayList<>();
		disconnectedRounds = new ArrayList<>();
	}
	
	XBotRound initiatorRound() {
		return initiatorRound;
	}
	
	XBotRound oldRound(InetSocketAddress initiator) {
		return getRound(Role.old, initiator);
	}
	
	XBotRound candidateRound(InetSocketAddress initiator) {
		return getRound(Role.candidate, initiator);
	}
	
	XBotRound disconnectedRound(InetSocketAddress initiator) {
		return getRound(Role.disconnected, initiator);
	}
	
	XBotRound getRound(XBotRound.Role localRole, InetSocketAddress initiator) {
		List<XBotRound> rounds;
		if(localRole == Role.old)
			rounds = oldRounds;
		else if(localRole == Role.candidate)
			rounds = candidateRounds;
		else if(localRole == Role.disconnected)
			rounds = disconnectedRounds;
		else if(localRole == Role.initiator)
			return initiatorRound;
		else
			return null;
		
		if(rounds == null)
			return null;
		else {
			for(XBotRound round : rounds)
				if(round.initiator.equals(initiator))
					return round;
			
			return null;
		}
	}
}
