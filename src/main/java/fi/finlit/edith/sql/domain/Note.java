package fi.finlit.edith.sql.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.joda.time.DateTime;

import fi.finlit.edith.Identifiable;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Note implements Identifiable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String lemma;

    @ManyToOne
    @Cascade(value = CascadeType.SAVE_UPDATE)
    private Term term;

    private Long editedOn;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<User> allEditors = new HashSet<User>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "note")
    private Set<NoteComment> comments = new HashSet<NoteComment>();

    @ManyToOne
    private User lastEditedBy;

    private String sources;

    private String subtextSources;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private NoteStatus status = NoteStatus.INITIAL;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    private Set<NoteType> types = new HashSet<NoteType>();

    @Enumerated(EnumType.STRING)
    private NoteFormat format;

    private String lemmaMeaning;

    @ManyToOne
    @Cascade(value=CascadeType.SAVE_UPDATE)
    private Person person;

    @ManyToOne
    @Cascade(value=CascadeType.SAVE_UPDATE)
    private Place place;

    private int documentNoteCount = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public Term getTerm() {
        return term;
    }

    public void setTerm(Term term) {
        this.term = term;
    }

    public Long getEditedOn() {
        return editedOn;
    }
    
    public DateTime getEditedOnDate() {
        return new DateTime(editedOn);
    }

    public void setEditedOn(Long editedOn) {
        this.editedOn = editedOn;
    }

    public Set<User> getAllEditors() {
        return allEditors;
    }

    public void setAllEditors(Set<User> allEditors) {
        this.allEditors = allEditors;
    }

    public Set<NoteComment> getComments() {
        return comments;
    }

    public void setComments(Set<NoteComment> comments) {
        this.comments = comments;
    }

    public void addComment(NoteComment comment) {
        this.comments.add(comment);
    }

    public void removeComment(NoteComment comment) {
        this.comments.remove(comment);
    }

    public User getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(User lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public String getSubtextSources() {
        return subtextSources;
    }

    public void setSubtextSources(String subtextSources) {
        this.subtextSources = subtextSources;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NoteStatus getStatus() {
        return status;
    }

    public void setStatus(NoteStatus status) {
        this.status = status;
    }

    public Set<NoteType> getTypes() {
        return types;
    }

    public void setTypes(Set<NoteType> types) {
        this.types = types;
    }

    public NoteFormat getFormat() {
        return format;
    }

    public void setFormat(NoteFormat format) {
        this.format = format;
    }

    public String getLemmaMeaning() {
        return lemmaMeaning;
    }

    public void setLemmaMeaning(String lemmaMeaning) {
        this.lemmaMeaning = lemmaMeaning;
    }

    public int getDocumentNoteCount() {
        return documentNoteCount;
    }

    public void setDocumentNoteCount(int documentNoteCount) {
        this.documentNoteCount = documentNoteCount;
    }

    public void decDocumentNoteCount() {
        if (this.documentNoteCount > 0) {
            this.documentNoteCount--;
        }
    }

    public void incDocumentNoteCount() {
        this.documentNoteCount++;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public Person getPerson() {
        return person;
    }

    public Place getPlace() {
        return place;
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static String createLemmaFromLongText(String text) {
        String result = null;
        if (WHITESPACE.matcher(text).find()) {
            String[] words = StringUtils.split(text);
            if (words.length == 2) {
                result = words[0] + " " + words[1];
            } else if (words.length > 1) {
                result = words[0] + " \u2013 \u2013 " + words[words.length - 1];
            } else {
                result = words[0];
            }
        } else {
            result = text;
        }
        return result;
    }

    public void addEditor(User user) {
       allEditors.add(user); 
    }
    
    public String getEditors() {
        Collection<String> result = new ArrayList<String>();
        for (User user : getAllEditors()) {
            if (getLastEditedBy().equals(user)) {
                result.add(user.getUsername());
            }
        }
        return StringUtils.join(result, ", ");
    }

}
