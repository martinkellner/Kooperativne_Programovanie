package rtsharecode.common.listeners;

import java.util.concurrent.TimeUnit;

import rtsharecode.common.Activator;
/**
 * Trieda ResourceChangeBlocker - blokovanie nevyžiadaných zmien štruktúry
 * @author Martin Kellner
 *
 */
public class ResourceChangeBlocker implements Runnable {
	
	/**
	 * beh vlánka - kontrola, èi nenastala zmena štruktúry, prípadne blokovanie.
	 */
	@Override
	public void run() {
		while (true) {
			int requestInfo = Activator.getDefault().getManagerListener().requestForBlocker();
			if ( Activator.getDefault().getManagerListener() != null && Activator.getDefault().getManagerListener().isInteruptBlocker() ) {
				break;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if ( Activator.getDefault().getManagerListener() != null && Activator.getDefault().getManagerListener().isInteruptBlocker() ) {
				break;
			}
			if (Activator.getDefault().getManagerListener() != null && Activator.getDefault().getManagerListener().requestForBlocker() != requestInfo) {
				for (int i = 0; i < 200; i ++) {
					try {
						TimeUnit.MILLISECONDS.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if ( Activator.getDefault().getManagerListener() != null && Activator.getDefault().getManagerListener().isInteruptBlocker() ) {
						return;
					}
					if ( Activator.getDefault().getManagerListener().isNonSendingChange() == false ) {
						Activator.getDefault().getManagerListener().setNonSendingChange(true);
						break;
					}
				}				
			}
		}
	}
}