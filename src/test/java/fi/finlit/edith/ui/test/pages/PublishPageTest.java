package fi.finlit.edith.ui.test.pages;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.internal.services.ArrayEventContext;
import org.apache.tapestry5.ioc.annotations.Autobuild;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fi.finlit.edith.domain.Document;
import fi.finlit.edith.ui.pages.document.PublishPage;
import fi.finlit.edith.ui.services.DocumentRepository;
import fi.finlit.edith.ui.test.services.AbstractServiceTest;
import fi.finlit.edith.ui.test.services.ServiceTestModule;

@Ignore
public class PublishPageTest extends AbstractServiceTest{
    
    @Autobuild
    @Inject
    private PublishPage publishPage;
    
    @Inject
    @Symbol(ServiceTestModule.TEST_DOCUMENT_KEY)
    private String testDocument;
    
    @Inject
    private DocumentRepository repository;
    
    @Inject
    private TypeCoercer typeCoercer;
    
    @Before
    public void setUp() throws IOException{
        Document document = repository.getDocumentForPath(testDocument);
        EventContext context = new ArrayEventContext(typeCoercer, new Object[]{document.getId()});
        publishPage.onActivate(context);
    }
    
    @Test
    public void onActionFromPublish() throws IOException, XMLStreamException{
        publishPage.onActionFromPublish("X");
    }

}
