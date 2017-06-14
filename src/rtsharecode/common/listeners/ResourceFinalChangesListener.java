package rtsharecode.common.listeners;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;

import rtsharecode.common.Activator;
import rtsharecode.common.sharing.MasterSharing;

/**
 * Trieda ResourceFinalChangeListener - spúštanie synchronizácie po zmene štruktúry
 * @author Martin Kellner
 *
 */
public class ResourceFinalChangesListener extends Object implements Runnable {
	
	/**
	 * beh vlákna - identifikácia poslednej zmeny štruktúry, volanie funkcionality pre synchonizáciu po zmene štriktúry
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