/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package auctionServer;

import java.util.TimerTask;

/**
 *
 * @author Bernhard
 */
public class GroupBidDenial extends TimerTask {
	private User user;
	
	public GroupBidDenial(User user) {
		this.user = user;
		Groupbid.addDenial(user, 1);
	}

	@Override
	public void run() {
		Groupbid.addDenial(user, -1);
	}
	
}
