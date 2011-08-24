package fi.finlit.edith.ui.components.note;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;

import fi.finlit.edith.EDITH;
import fi.finlit.edith.dto.NoteSearchInfo;
import fi.finlit.edith.dto.OrderBy;
import fi.finlit.edith.dto.UserInfo;
import fi.finlit.edith.sql.domain.Document;
import fi.finlit.edith.sql.domain.NoteFormat;
import fi.finlit.edith.sql.domain.NoteType;
import fi.finlit.edith.sql.domain.TermLanguage;
import fi.finlit.edith.ui.pages.document.Annotate;
import fi.finlit.edith.ui.services.UserDao;

@SuppressWarnings("unused")
public class NoteSearchForm {

    @InjectPage
    private Annotate page;

    private final Collection<Document> selectedDocuments = new ArrayList<Document>();

    @Property
    private NoteType type;

    @Property
    private NoteFormat format;

    @Property
    private UserInfo user;

    @Inject
    private UserDao userDao;

    @Property
    private OrderBy loopedOrderBy;

    @Inject
    @Symbol(EDITH.EXTENDED_TERM)
    private boolean slsMode;

    public int getSelectedDocumentCount() {
        return selectedDocuments != null ? selectedDocuments.size() : 0;
    }

    Object onSuccessFromNoteSearchForm() {
        return page.getSearchResults().getBlock();
    }

    public Collection<UserInfo> getUsers() {
        return userDao.getAllUserInfos();
    }

    public NoteType[] getTypes() {
        return NoteType.values();
    }

    public NoteFormat[] getFormats() {
        return NoteFormat.values();
    }

    public boolean isTypeSelected() {
        return page.getSearchInfo().getNoteTypes().contains(type);
    }

    public void setTypeSelected(boolean selected) {
        if (selected) {
            page.getSearchInfo().getNoteTypes().add(type);
        } else {
            getSearchInfo().getNoteTypes().remove(type);
        }
    }

    public boolean isFormatSelected() {
        return getSearchInfo().getNoteFormats().contains(format);
    }

    public void setFormatSelected(boolean selected) {
        if (selected) {
            getSearchInfo().getNoteFormats().add(format);
        } else {
            getSearchInfo().getNoteFormats().remove(format);
        }
    }

    public boolean isUserSelected() {
        return getSearchInfo().getCreators().contains(user);
    }

    public void setUserSelected(boolean selected) {
        if (selected) {
            getSearchInfo().getCreators().add(user);
        } else {
            getSearchInfo().getCreators().remove(user);
        }
    }

    public void setOrderBy(OrderBy orderBy) {
        getSearchInfo().setOrderBy(orderBy);
    }

    public OrderBy getOrderBy() {
        if (getSearchInfo().getOrderBy() == null) {
            getSearchInfo().setOrderBy(slsMode ? OrderBy.KEYTERM : OrderBy.LEMMA);
        }
        return getSearchInfo().getOrderBy();
    }

    public OrderBy[] getOrderBys() {
        if (slsMode) {
            return new OrderBy[] {OrderBy.KEYTERM, OrderBy.USER, OrderBy.STATUS, OrderBy.DATE};
        }
        return new OrderBy[] {OrderBy.LEMMA, OrderBy.USER, OrderBy.STATUS, OrderBy.DATE};
    }

    public boolean isReversed() {
        return !getSearchInfo().isAscending();
    }

    public void setReversed(boolean reversed) {
        getSearchInfo().setAscending(!reversed);
    }

    public boolean isOrphans() {
        return getSearchInfo().isOrphans();
    }

    public void setOrphans(boolean orphans) {
        getSearchInfo().setOrphans(orphans);
    }

    public TermLanguage getLanguage() {
        return getSearchInfo().getLanguage();
    }

    public void setLanguage(TermLanguage lang) {
        getSearchInfo().setLanguage(lang);
    }

    public NoteSearchInfo getSearchInfo() {
        return page.getSearchInfo();
    }

    public String getDocuments() {
        Collection<Long> documentIds = new ArrayList<Long>();
        for (Document document : getSearchInfo().getDocuments()) {
            documentIds.add(document.getId());
        }
        return StringUtils.join(documentIds, ",");
    }

    public void setDocuments(String documents) {
        getSearchInfo().getDocuments().clear();
        for (String documentId : documents.split(",")) {
            Document document = new Document();
            document.setId(Long.valueOf(documentId));
            getSearchInfo().getDocuments().add(document);
        }
    }

    public String getFullText() {
        return getSearchInfo().getFullText();
    }

    public void setFullText(String fullText) {
        getSearchInfo().setFullText(fullText);
    }

    public boolean getIncludeAllDocs() {
        return getSearchInfo().isIncludeAllDocs();
    }

    public void setIncludeAllDocs(boolean value) {
        getSearchInfo().setIncludeAllDocs(value);
    }


}
