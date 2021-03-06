package com.mysema.edith.ui.components.note;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.mysema.edith.domain.NameForm;
import com.mysema.edith.domain.NoteFormat;
import com.mysema.edith.domain.Person;
import com.mysema.edith.domain.Place;
import com.mysema.edith.domain.Term;
import com.mysema.edith.domain.TermLanguage;
import com.mysema.edith.services.PersonDao;
import com.mysema.edith.services.PlaceDao;
import com.mysema.edith.services.TermDao;
import com.sun.xml.internal.ws.api.PropertySet.Property;

@SuppressWarnings("unused")
public class SKSNoteForm extends AbstractNoteForm {
    @Inject
    @Property
    private Block editPersonForm;

    @Inject
    @Property
    private Block editPlaceForm;

    @Inject
    @Property
    private Block editTermForm;

    @Property
    private NameForm loopPerson;

    @Property
    private NameForm loopPlace;

    @InjectComponent
    @Property
    private Zone personZone;

    @InjectComponent
    @Property
    private Zone placeZone;

    @InjectComponent
    @Property
    private Zone termZone;

    private Place place;

    @Property
    private Long placeId;

    @Property
    private Long termId;

    @Inject
    private PlaceDao placeDao;

    private Person person;

    @Property
    private Long personId;

    @Inject
    private PersonDao personDao;

    private Term term;

    @Inject
    private TermDao termDao;

    @Inject
    @Property
    private Block closeDialog;

    @Override
    void onPrepareFromNoteEditForm(long noteId) {

        super.onPrepareFromNoteEditForm(noteId);

        if (!isPerson()) {
            setPerson(getNoteOnEdit().getPerson());
        }
        if (!isPlace()) {
            setPlace(getNoteOnEdit().getPlace());
        }

        if (!isTerm()) {
            setTerm(getNoteOnEdit().getTerm());
        }

    }

    @Override
    Object onSuccessFromNoteEditForm() {
        getNoteOnEdit().setPerson(getPerson());
        getNoteOnEdit().setPlace(getPlace());
        getNoteOnEdit().setTerm(getTerm());
        return super.onSuccessFromNoteEditForm();
    }

    public String getTimeOfBirth() {
        if (isPerson() && getPerson().getTimeOfBirth() != null) {
            return getPerson().getTimeOfBirth().asString();
        }
        return null;
    }

    public String getTimeOfDeath() {
        if (isPerson() && getPerson().getTimeOfDeath() != null) {
            return getPerson().getTimeOfDeath().asString();
        }
        return null;
    }

    public boolean isPerson() {
        if (person == null && personId != null) {
            person = personDao.getById(personId);
        }
        return person != null;
    }

    public boolean isTerm() {
        if (term == null && termId != null) {
            term = termDao.getById(termId);
        }
        return term != null;
    }

    public boolean isPlace() {
        if (place == null && placeId != null) {
            place = placeDao.getById(placeId);
        }
        return place != null;
    }

    private void setPerson(Person person) {
        this.person = person;
        if (isPerson()) {
            personId = person.getId();
        }
    }

    private void setPlace(Place place) {
        this.place = place;
        if (isPlace()) {
            placeId = place.getId();
        }
    }

    private void setTerm(Term term) {
        this.term = term;
        if (isTerm()) {
            termId = term.getId();
        }
    }

    private Person getPerson() {
        if (personId != null) {
            return personDao.getById(personId);
        }
        return person;
    }

    private Term getTerm() {
        if (termId != null) {
            return termDao.getById(termId);
        }
        return term;
    }

    Collection<Term> onProvideCompletionsFromTerm(String partial) {
        return termDao.findByStartOfBasicForm(partial, 10);
    }

    Collection<Person> onProvideCompletionsFromPerson(String partial) {
        return personDao.findByStartOfFirstAndLastName(partial, 10);
    }

    Collection<Place> onProvideCompletionsFromPlace(String partial) {
        return placeDao.findByStartOfName(partial, 10);
    }

    public String getNormalizedDescription() {
        if (isPerson()) {
            return getPerson().getNormalized().getDescription();
        }
        return null;
    }

    public String getNormalizedFirst() {
        if (isPerson()) {
            return getPerson().getNormalized().getFirst();
        }
        return null;
    }

    public String getNormalizedLast() {
        if (isPerson()) {
            return getPerson().getNormalized().getLast();
        }
        return null;
    }

    public String getNormalizedPlaceDescription() {
        if (isPlace()) {
            return getPlace().getNormalized().getDescription();
        }
        return null;
    }

    public String getNormalizedPlaceName() {
        if (isPlace()) {
            return getPlace().getNormalized().getName();
        }
        return null;
    }

    public int getPersonInstances() {
        if (isPerson()) {
            return getDocumentNoteDao().getOfPerson(getPerson().getId()).size();
        }
        return 0;
    }

    public Set<NameForm> getPersons() {
        if (isPerson()) {
            return getPerson().getOtherForms();
        }
        return new HashSet<NameForm>();
    }

    private Place getPlace() {
        if (placeId != null) {
            return placeDao.getById(placeId);
        }
        return place;
    }

    public int getPlaceInstances() {
        if (isPlace()) {
            return getDocumentNoteDao().getOfPlace(getPlace().getId()).size();
        }
        return 0;
    }

    public Set<NameForm> getPlaces() {
        if (isPlace()) {
            return getPlace().getOtherForms();
        }
        return new HashSet<NameForm>();
    }

    Object onEditPerson(Long id) {
        personId = id;
        return editPersonForm;
    }

    Object onEditPlace(Long id) {
        placeId = id;
        return editPlaceForm;
    }

    Object onEditTerm(Long id) {
        termId = id;
        return editTermForm;
    }

    Object onPerson(long id) {
        if (!isPerson()) {
            setPerson(personDao.getById(id));
        }
        return personZone.getBody();
    }

    Object onPlace(long id) {
        if (!isPlace()) {
            setPlace(placeDao.getById(id));
        }
        return placeZone.getBody();
    }

    Object onTerm(long id) {
        if (!isTerm()) {
            setTerm(termDao.getById(id));
        }
        return termZone.getBody();
    }

    @Validate("required")
    public NoteFormat getFormat() {
        return getNoteOnEdit().getFormat();
    }

    public void setFormat(NoteFormat noteFormat) {
        super.getNoteOnEdit().setFormat(noteFormat);
    }

    public String getTermBasicForm() {
        if (isTerm()) {
            return getTerm().getBasicForm();
        }
        return null;
    }

    public String getTermMeaning() {
        if (isTerm()) {
            return getTerm().getMeaning();
        }
        return null;
    }

    public TermLanguage getTermLanguage() {
        if (isTerm()) {
            return getTerm().getLanguage();
        }
        return null;
    }
}
