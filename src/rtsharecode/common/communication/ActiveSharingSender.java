package rtsharecode.common.communication;

import rtsharecode.common.sharing.MasterSharing;
/**
 * Trieda ActiveSharingSender - odosiela informáciu o aktívnom zdie¾aní.
 * @author Martin Kellner
 *
 */
public class ActiveSharingSender extends Object implements Runnable {
	
	/**
	 * beh vlákna - odosiela, každých 4 sekúnd informáciu o aktívnom zdie¾aní.
	 */
	@Override
	public void run() {
		while (true) {
			MasterSharing.getInstance().sendActiveSharingStatus();
			if (Messenger.getInstance().getInteruptSender()) {
				break;
			}
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Messenger.getInstance().terminateActiveSharingSender();
	}	
}
