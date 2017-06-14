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
 * Trieda SyncUserItem - vykreslenie informácií o synchronizovanom používate¾ovi.
 * @author Martin Kellner
 *
 */
public class SyncUserItem implements ViewItem{

	private String userName;
	private Composite composite;
	private boolean hasAccess = false;
	private Button accessButton = null;
	private boolean isEnabled = true;
	private boolean wantAccess = false;
	
	@Override
	/**
	 * Vráti meno používate¾a.
	 * @return String adminName
	 */
	public String getUserName() {
		return this.userName;
	}
	
	/**
	 * Vráti true, ak má prístup, inak false.
	 * @return boolean 
	 */
	public boolean isHasAccess() {
		return hasAccess;
	}


	@Override
	/**
	 * 
	 * Nastaví meno používate¾a.
	 */
	public void setUserName(String name) {
		this.userName = name;
	}

	@Override
	/**
	 * Kreslenie komponentov do View.
	 */
	public void draw() {
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(20, 20));
		Image image0 = new Image(composite.getDisplay(), SyncUserItem.class.getResourceAsStream("images/user.png"));
		label.setImage(image0);
		Label label0 = new Label(composite, SWT.NONE);
		label0.setText(this.userName);
		label0.setFont(new Font(label0.getDisplay(), "Tahoma",7, SWT.BOLD));
		accessButton = new Button(composite, SWT.PUSH);
		accessButton.setLayoutData(new GridData(105, 23));
	    if (!this.hasAccess) {
	    	if (this.wantAccess) {
	    		accessButton.setImage(new Image(accessButton.getDisplay(), new ImageData(SyncUserItem.class.getResourceAsStream("images/button_access_con.png"))));
	    	} else {
	    		accessButton.setImage(new Image(accessButton.getDisplay(), new ImageData(SyncUserItem.class.getResourceAsStream("images/button_access.png"))));
	    	}			
		} else {
			accessButton.setImage(new Image(accessButton.getDisplay(), new ImageData(SyncUserItem.class.getResourceAsStream("images/button_access_back.png"))));
		}
		this.accessButton.setEnabled(this.isEnabled);
		createButtonListener(accessButton);
	}
	
	/**
	 * Vytvorí tlaèidlo
	 */
	private void createButtonListener(Button button) {
		button.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (hasAccess) {
					hasAccess = false;
					MasterSharing.getInstance().removeAccess(userName);
					Activator.getDefault().getView().setEnableOtherAccessButton(userName);
					if (!wantAccess) {
						accessButton.setImage(new Image(accessButton.getDisplay(), new ImageData(SyncUserItem.class.getResourceAsStream("images/button_access.png"))));
					} else {
						accessButton.setImage(new Image(accessButton.getDisplay(), new ImageData(SyncUserItem.class.getResourceAsStream("images/button_access_con.png"))));
					}
				} else {
					if (MasterSharing.getInstance().controlAccess(userName)) {
						hasAccess = true;
						Activator.getDefault().getView().setDisableOtherAccessButton(userName);
						accessButton.setImage(new Image(accessButton.getDisplay(), new ImageData(SyncUserItem.class.getResourceAsStream("images/button_access_back.png"))));
					}
				}
			}
			
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
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
	 * Zakáže staèenie tlaèidla
	 */
	protected void setDisableButton() {
		this.accessButton.setEnabled(false);
		this.isEnabled = false;
	}
	
	/**
	 * Povolí stlaèenie tlaèidla
	 */
	protected void setEnableButton() {
		this.accessButton.setEnabled(true);
		this.isEnabled = true;
	}
	
	/**
	 * Nastavi hodnotu wantAccess
	 * @param boolean value
	 */
	protected void setWantAccess(boolean value) {
		this.wantAccess = value;		
	}
}