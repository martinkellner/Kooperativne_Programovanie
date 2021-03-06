package rtsharecode.common.gui.view;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface ViewItem - pre SyncUserItem, WaitUserItem a ActiveAdminItem
 * @author Martin Kellner
 *
 */
public interface ViewItem {
	
	/**
	 * Vráti meno používateľa.
	 * @return String meno
	 */
	public String getUserName();
	
	/**
	 * 
	 * Nastaví meno používateľa.
	 */
	public void setUserName(String name);
	
	/**
	 * Kreslenie komponentov do View.
	 */
	public void draw();
	
	/**
	 * Nastavenie referencie na grafickú plochu.
	 */
	public void setComposite( Composite composite );
}