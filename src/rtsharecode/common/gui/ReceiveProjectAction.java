package rtsharecode.common.gui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import rtsharecode.common.Activator;
import rtsharecode.common.communication.Connection;
import rtsharecode.common.sharing.MasterSharing;
import rtsharecode.common.sharing.CommonSharing;
/**
 * Trieda ReceiveProjectAction - z�skanie projektu pri pravom kliku na projekt v projekt-browser.
 * @author a641813
 *
 */
public class ReceiveProjectAction  implements IObjectActionDelegate {

	private Shell shell;
    private IProject project;

    /**
     * Over� pr�va pre zdie�ania, nastav� projekt a vol� funkcionalitu pre inicializ�ciu zdie�ania.
     */
    @Override
    public void run( IAction arg0 ) {
      	if ( Connection.getInstance().isLogin() && Connection.getInstance().isAdmin() ) {
      		if ( project == null ) {
      			MessageDialog.openError( shell, " Projekt nebol z�skan�. ", " Vybran� projekt sa nepodarilo z�ska�. " );
      		} else {
      			if (CommonSharing.getInstance().isShared()) {
      				MessageDialog.openError(shell, "Nie je mo�n� zdie�a� projekt", "Projekt nie je mo�n� zdie�a�, najsk�r preru�te aktu�lne zdie�anie.");
      				return;
      			}
      			Activator.getDefault().setProject( project );
      			Display.getDefault().asyncExec( new Runnable() {
					
					@Override
					public void run() {
						trySharingProject( );
					}
				});      			
      		}
      	} else if ( ! Connection.getInstance().isLogin( ) ) {
      		MessageDialog.openWarning( shell, " Nie ste prihl�sen�. ", " Ak chcete zdie�a� projekt mus�te by� prihl�sen� pod u�ite�sk�m kontom. " );
      	} else if ( ! Connection.getInstance().isAdmin() ){
      		MessageDialog.openWarning( shell, " Nie ste administr�torom. ", " Ste prihl�sen� ako pou��vate�: " + Connection.getInstance().getLoginName() + ". Tento pou��vate� nem� administr�torsk� pr�va. " );
      	}
    }

    @Override
    public void selectionChanged( IAction arg0, ISelection arg1 ) {}

    /**
     * Odchyt� projekt
     */
    @Override
    public void setActivePart( IAction action, IWorkbenchPart part ) {
      	if ( Connection.getInstance().isLogin( ) && Connection.getInstance().isAdmin( ) ) {
      		this.shell = part.getSite( ).getShell( );
        	IStructuredSelection structuredSelection = ( IStructuredSelection ) part.getSite( ).getSelectionProvider( ).getSelection( );
        	Object selected = structuredSelection.getFirstElement( );
        	if ( selected instanceof IProject ) {
           		project = ( IProject ) selected;
        	} else if ( selected instanceof IAdaptable ) {
           		project = ( IProject ) ( ( IAdaptable ) selected ).getAdapter( IProject.class );
        	} else {
           		project = null;
        	}
      	}
    }
    
    private void trySharingProject( ) {
    	MasterSharing.getInstance().initSharing();
	}
    
    public IProject getProject() {
    	return project;
    }
}