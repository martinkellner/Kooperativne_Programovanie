package rtsharecode.common.gui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import rtsharecode.common.Activator;

/**
 * Trieda PreferencesPace - nastavenia pre plugin
 * @author Martin Kellner
 *
 */
public class PreferencePage extends FieldEditorPreferencePage implements 
	IWorkbenchPreferencePage {
   
	/**
	 * Konštruktor
	 */
	public PreferencePage() {
		super( GRID );
        setPreferenceStore( Activator.getDefault( ).getPreferenceStore( ) );
        setDescription( "Nastavenia ShareCode plugin-u" );	
	}	
	
	/**
	 * Vytovrenie položiek pre vyplnenie hodnôt.
	 */
	public void createFieldEditors() {
		StringFieldEditor server = new StringFieldEditor(PreferencesConstants.XMPP_SERVER, "Adresa XMPP servera: ",
                getFieldEditorParent( ) );
		StringFieldEditor login = new StringFieldEditor(PreferencesConstants.XMPP_LOGIN, "Prihlasovacie meno: ",
                getFieldEditorParent( ) );
		StringFieldEditor password = new StringFieldEditor(PreferencesConstants.XMPP_PASSWORD, "Prihlasovacie heslo: ",
                getFieldEditorParent( ) );
		StringFieldEditor group = new StringFieldEditor(PreferencesConstants.XMPP_GROUP, "Skupina",
				getFieldEditorParent( ) );
		server.setEmptyStringAllowed( false );
		login.setEmptyStringAllowed( false );
		group.setEmptyStringAllowed( false );
		password.setEmptyStringAllowed( false );
		password.getTextControl( getFieldEditorParent( ) ).setEchoChar( '*' );
		addField( server );
		addField( login );
		addField( password );
		addField( group );
	}        
	
	@Override
	public void init( IWorkbench workbench ) { }
}
