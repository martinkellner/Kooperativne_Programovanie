package rtsharecode.common.gui.view;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface ViewItem - pre SyncUserItem, WaitUserItem a ActiveAdminItem
 * @author Martin Kellner
 *
 */
public interface ViewItem {
	
	/**
	 * Vr�ti meno pou��vate�a.
	 * @return String meno
	 */
	public String getUserName();
	
	/**
	 * 
	 * Nastav� meno pou��vate�a.
	 */
	public void setUserName(String name);
	
	/**
	 * Kreslenie komponentov do View.
	 */
	public void draw();
	
	/**
	 * Nastavenie referencie na grafick� plochu.
	 */
	public void setComposite( Composite composite );
}