package rtsharecode.common.gui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import rtsharecode.common.sharing.CommonSharing;

/**
 * Trieda ActiveAdminItem - vykreslenie inform·ciÌ o uËiteæovi, ktorÌ zdieæa do View.
 * @author Martin Kellner
 *
 */
public class ActiveAdminLabel implements ViewItem {
	
	private String adminName;
	private Composite composite;
	private Button control;
	private boolean requestInvation = false;
	private boolean requestSync = false;
	private boolean can = false;
	
	@Override
	/**
	 * Vr·ti meno uËiteæa.
	 * @return String adminName
	 */
	public String getUserName() {
		return this.adminName;
	}

	
	@Override
	/**
	 * 
	 * NastavÌ meno uËiteæa.
	 */
	public void setUserName(String name) {
		this.adminName = name;
	}

	@Override
	/**
	 * Kreslenie komponentov do View.
	 */
	public void draw() {
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(20, 20));
		Image image0 = new Image(composite.getDisplay(), SyncUserItem.class.getResourceAsStream( "images/user.png" ) );
		label.setImage(image0);
		Label label0 = new Label(composite, SWT.NONE);
		label0.setText(this.adminName);
		label0.setFont(new Font(label0.getDisplay(), "Tahoma",7, SWT.BOLD ));
		control = new Button(composite, SWT.PUSH);
		control.setLayoutData(new GridData(105, 23));
		
		if ( !requestSync ) {
			control.setImage(new Image(composite.getDisplay(), new ImageData(ActiveAdminLabel.class.getResourceAsStream("images/button_invation.png"))));
			if ( this.requestInvation ) {
				control.setEnabled(false);
			}
		} else {
			control.setImage(new Image(composite.getDisplay(), new ImageData(ActiveAdminLabel.class.getResourceAsStream("images/button_want.png"))));
			if ( !this.can ) {
				control.setEnabled(false);
			}
		}
		createButtonListener();
	}
	
	/**
	 * Nastavenie hodnotu, Ëi bola odostan· poûiadavka na pozvanie.
	 * @param boolean requestInvation
	 */
	public void setRequestInvation(boolean requestInvation) {
		this.requestInvation = requestInvation;
	}

	/**
	 * Nastavenie hodnotu, Ëi bola odostan· poûiadavka na synchoniz·ciu.
	 * @param boolean requestSync
	 */
	public void setRequestSync(boolean requestSync) {
		this.requestSync = requestSync;
	}
	
	/**
	 * VytvorÌ tlaËidlo
	 */
	private void createButtonListener() {
		control.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (!requestInvation) {
					if (CommonSharing.getInstance().sendInvation(adminName)) {
						requestInvation = true;
						control.setEnabled(false);
					}
				} else if (can) {
					if ( CommonSharing.getInstance().sendAccess(adminName) ) {
						requestSync = true;
						control.setEnabled(false);
						can = false;
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
	
	/**
	 * Nastavenie hodnoty, Ëi mÙûe pouûÌvateæ ûiadaù o prÌstup k zdieæaniu.
	 * @param b
	 */
	protected void setCan(boolean b) {
		this.can = b;
	}
	
	/**
	 * Nastavenie referencie na grafick˙ plochu.
	 */
	@Override
	public void setComposite(Composite composite) {
		this.composite = composite;
	}
	
	/**
	 * VrÌti hodnotu, Ëi mÙûe pouûÌvateæ ûiadaù o prÌstup k zdieæaniu.
	 * @return boolean true, ak moûe, inak false.
	 */
	public boolean isCan() {
		return this.can;
	}
}