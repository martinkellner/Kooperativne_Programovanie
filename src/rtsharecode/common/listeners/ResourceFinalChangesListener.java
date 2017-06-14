package rtsharecode.common.listeners;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;

import rtsharecode.common.Activator;
import rtsharecode.common.sharing.MasterSharing;

/**
 * Trieda ResourceFinalChangeListener - sp��tanie synchroniz�cie po zmene �trukt�ry
 * @author Martin Kellner
 *
 */
public class ResourceFinalChangesListener extends Object implements Runnable {
	
	/**
	 * beh vl�kna - identifik�cia poslednej zmeny �trukt�ry, volanie funkcionality pre synchoniz�ciu po zmene �trikt�ry
	 */
	@Override
	public void run() {
		while ( true ) {
			if ( Activator.getDefault().getManagerListener() != null && Activator.getDefault().getManagerListener().isInteruptChecker() ) {
				break;
			}
			int i = Activator.getDefault().getManagerListener().resourcesChanges.size();
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if ( Activator.getDefault().getManagerListener() != null && Activator.getDefault().getManagerListener().isInteruptChecker() ) {
				break;
			}
			int j = Activator.getDefault().getManagerListener().resourcesChanges.size();
			if (i == j) {
				if (Activator.getDefault().getManagerListener().isNonSendingChange() != true) {
					Activator.getDefault().refreshListener();
					Display.getDefault().asyncExec( new Runnable() {
							
						@Override
						public void run() {
							try {
								MasterSharing.getInstance().syncStructure();	
							} catch (CoreException | IOException e) {
								e.printStackTrace();
							}	
						}
					});
				}
				Activator.getDefault().getManagerListener().resourcesChanges.clear();
				break;
			}
		}
	}
}