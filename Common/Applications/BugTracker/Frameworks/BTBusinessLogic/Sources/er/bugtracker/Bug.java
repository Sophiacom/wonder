// Bug.java
// 
package er.bugtracker;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSValidation;
import er.corebusinesslogic.ERCoreBusinessLogic;
import er.extensions.eof.EOEnterpriseObjectClazz;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.logging.ERXLogger;

public class Bug extends _Bug {
    static final ERXLogger log = ERXLogger.getERXLogger(Bug.class);

    public boolean _componentChanged;
    public boolean _ownerChanged;

    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
        setPriority(Priority.MEDIUM);
        setState(State.ANALYZE);
        addToBothSidesOfTargetRelease(Release.clazz.defaultRelease(ec));
        setReadAsBoolean(true);
        setDateSubmitted(new NSTimestamp());
        setDateModified(new NSTimestamp());
        setFeatureRequest(new Integer(0));
    }

    public void markReadBy(People reader) {
        if (owner() != null && owner().equals(localInstanceOf(reader)) && !isReadAsBoolean()) {
            setReadAsBoolean(true);
            editingContext().saveChanges();
        }
    }
    
    public void markUnread() {
        setReadAsBoolean(false);
    }

    public void touch() {
        markUnread();
        setDateModified(new NSTimestamp());
    }

    public void setReadAsBoolean(boolean read) {
        setRead(read ? "Y":"N");
    }
    public boolean isReadAsBoolean() {
        return "Y".equals(read());
    }

    // FIXME:(ak) now *what* is this supposed to do???
    public void setComponent(Component value) {
        willChange();
        Component oldComponent = component();
        super.setComponent(value);
        if (value!=null) {
            if (owner() == null) {
                addToBothSidesOfOwner(component().owner());
            } else if ((oldComponent==null) || (!(value.equals(oldComponent)))) {
                _componentChanged = true;
            }
        }
    }

    public void setOwner(People value) {
        willChange();
        People oldOwner = owner();
        super.setOwner(value);
        EOEnterpriseObject localOwner = ERCoreBusinessLogic.actor(editingContext());
        if ((value!=null) && (value!=localOwner) && (oldOwner==null ||
                                                     (!(value.equals(oldOwner))))) {
            _ownerChanged=true;
            if (oldOwner!=null) setPreviousOwner(oldOwner);
            touch();
        }
    }

    public boolean isFeatureRequest() { return ERXValueUtilities.booleanValue(featureRequest()); }

    public void setState(State newState) {
        willChange();
        State oldState = state();
        if (newState==State.CLOSED && isFeatureRequest() && oldState==State.VERIFY)
            newState=State.DOCUMENT;
        super.setState(newState);
        People documenter = People.clazz.defaultDocumenter(editingContext());
        if (documenter!=null && newState==State.DOCUMENT && !_ownerChanged) {
            setOwner((People)EOUtilities.localInstanceOfObject(editingContext(), documenter));
            setReadAsBoolean(false);
        }
        People verifier = People.clazz.defaultVerifier(editingContext());
        if (verifier!=null && newState==State.VERIFY && !_ownerChanged) {
            // setOwner((EOEnterpriseObject)valueForKey("originator")); // we send the bug back to its originator
            setOwner((People)EOUtilities.localInstanceOfObject(editingContext(),verifier));
            touch();
        }
    }

    public Object validateTargetReleaseForNewBugs() throws NSValidation.ValidationException {
        Release release = Release.clazz.targetRelease(editingContext());
        if (release != null) {
            if (!release.isOpenAsBoolean())
                throw new NSValidation.ValidationException("Sorry, the release <b>"+release.valueForKey("name")+"</b> is closed. Bugs/Requirements can only be attached to open releases" );
        }
        return null;
    }

    public void validateForInsert() {
        super.validateForInsert();
        validateTargetReleaseForNewBugs();
    }

    public void validateForUpdate() {
        if (_componentChanged && component()!=null && !_ownerChanged) {
            addToBothSidesOfOwner(component().owner());
        }
        _componentChanged=false;
        _ownerChanged=false;
        super.validateForUpdate();
        touch();

    }

    public void validateForDelete () throws NSValidation.ValidationException {
        throw new NSValidation.ValidationException("Bugs can not be deleted; they can only be closed.");
    }

    // this key is used during mass updates by both the template EO and the real bugs
    private String _newText;
    public String newText() { return _newText; }
    public void setNewText(String newValue) {
        _newText=newValue;
        if (newValue!=null && newValue.length()>0) {
            String oldText = textDescription();

            if(oldText!=null)
                oldText = oldText+"\n\n";
            else
                oldText = "";
            String newText=oldText+newValue;
            setTextDescription(newText);
            touch();
        }
    }

    public void didUpdate() {
        super.didUpdate();
        _newText=null;
    }
    
    
    // Class methods go here
    
    public static class BugClazz extends _BugClazz {
        
    }

    public static final BugClazz clazz = (BugClazz) EOEnterpriseObjectClazz.clazzForEntityNamed("Bug");
}

