package rtsharecode.common.communication;

import rtsharecode.common.sharing.MasterSharing;
/**
 * Trieda ActiveSharingSender - odosiela inform�ciu o akt�vnom zdie�an�.
 * @author Martin Kellner
 *
 */
public class ActiveSharingSender extends Object implements Runnable {
	
	/**
	 * beh vl�kna - odosiela, ka�d�ch 4 sek�nd inform�ciu o akt�vnom zdie�an�.
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
