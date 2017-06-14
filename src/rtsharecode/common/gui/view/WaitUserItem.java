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

import rtsharecode.common.Activator;
import rtsharecode.common.sharing.MasterSharing;
/**
 * Trieda WaitUserItem - vykreslenie informácií o èakujúcom používate¾ovi
 * @author Martin Kellner
 *
 */
public class WaitUserItem implements ViewItem {

	private String userName;
	private boolean isInvated = false;
	private boolean forSync = false;
	private Composite composite;
	private Button button;
	
	
	@Override
	/**
	 * Vráti meno používate¾a
	 * 
	 */
	public String getUserName() {
		return this.userName;
	}

	@Override
	/**
	 * Nastaví meno používate¾a
	 */
	public void setUserName(String name) {
		this.userName = name;
	}

	@Override
	/**
	 * Kreslenie komponentov do View
	 */
	public void draw() {
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(20, 20));
		Image image0 = new Image(composite.getDisplay(), SyncUserItem.class.getResourceAsStream( "images/user.png" ) );
		label.setImage(image0);
		Label label0 = new Label(composite, SWT.NONE);
		label0.setText(this.userName);
		label0.setFont(new Font(label0.getDisplay(), "Tahoma", 7, SWT.BOLD ));
		button = new Button(composite, SWT.PUSH);
		button.setLayoutData(new GridData(105, 23));
		if (this.forSync) {
			setForSync();
			this.button.setImage(new Image(this.button.getDisplay(), new ImageData( WaitUserItem.class.getResourceAsStream( "images/button_sync.png"))));
		} else {
			if ( !this.isInvated ) {
				button.setImage(new Image(this.button.getDisplay(), new ImageData( WaitUserItem.class.getResourceAsStream( "images/button_invate.png"))));
			} else {
				
				this.isInvated = true;
				button.setImage(new Image(this.button.getDisplay(), new ImageData( WaitUserItem.class.getResourceAsStream( "images/button_invated.png"))));
				button.setEnabled(false);
			}
		}
		createButtonListener(button);
	}
	
	/**
	 * Vytvorí tlaèidlo.
	 * @param button
	 */
	private void createButtonListener( Button button ) {
		button.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (forSync) {
					sync();
				} else {
					if ( !isInvated ) {
						invate();
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
	

	@Override
	/**
	 * Nastavenie referencie na grafickú plochu.
	 */
	public void setComposite(Composite composite) {
		this.composite = composite;
	}
	
	/**
	 * Nastavenie hodnoty, èi bol používate¾ už pozvaný.
	 * @param boolean value 
	 */
	public void setIsInvated( boolean value ) {
		this.isInvated = value;
	}
	
	/**
	 * Nastavenie hodnoty forSync na true 
	 */
	protected void setForSync() {
		this.forSync = true;
	}

	/**
	 * Vrati hodnotu forSync
	 * @return boolean
	 */
	protected boolean isForSync() {
		return this.forSync;
	}
	
	/**
	 * Vráti hodnotu isInvated
	 * @return boolean
	 */
	protected boolean isInvated() {
		return this.isInvated;
	}
	
	/**
	 * Volanie funkcionality pre pozvanie
	 */
	protected void invate() {
		button.setImage(new Image(this.button.getDisplay(), new ImageData( WaitUserItem.class.getResourceAsStream( "images/button_invated.png"))));
		button.setEnabled(false);
		isInvated = true;
		MasterSharing.getInstance().invate(this.userName);
	}
	
	/**
	 * Volanie funkcionality pre synchronizáciu
	 */
	protected void sync() {
		if (MasterSharing.getInstance().syncUser(userName)) {
			Activator.getDefault().getView().addUser(userName, "sync", "add", false , false);
		} else {
			Activator.getDefault().getView().addUser(userName, "wait", "remove", false , false);
		}
	}
}