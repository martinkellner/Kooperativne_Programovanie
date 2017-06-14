package rtsharecode.common.communication;

import rtsharecode.common.Activator;
import rtsharecode.common.sharing.CommonSharing;
/**
 * Trieda ActiveSharingListener - kontrola aktu�lnosti zdie�ania
 * @author Martin Kellner
 *
 */
public class ActiveSharingListener extends Object implements Runnable {
	
	/**
	 * funkcionalita vl�kna - kontroluje ka�d�ch 7 sekund aktu�lnos� zdie�ania.
	 */
	@Override
	public void run() {
		while(true) {
			int countOfRequest = CommonSharing.getInstance().getCountOfResponseIsSharing();
			if (Messenger.getInstance().getInteruptListner()) {
				break;
			}
			try {
				Thread.sleep(7000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (CommonSharing.getInstance().getCountOfResponseIsSharing() == countOfRequest) {
				Activator.getDefault().destroy();
			}
			if (Messenger.getInstance().getInteruptListner()) {
				break;
			}
		}
		Messenger.getInstance().terminateActiveSharingListener();
	}
}